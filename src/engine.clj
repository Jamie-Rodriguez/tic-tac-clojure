(ns engine
    (:gen-class)
    (:use [clojure.string :only (join)])
    (:use [clojure.pprint :only (cl-format)])
    (:use constants)
    (:use print)
    (:use [mcts :only (make-mcts-agent)]))


; Uses a modified version of Brian Kernighan’s Algorithm.
; Instead of just counting the number of set bits, writes the set bits into a list.
; Uses the fact that n & (n-1) removes the *rightmost* SET bit from n
; Then use n XOR (n & (n-1)) to get the removed bit, and add it to the list
; e.g.:
;     input: 1101
;     output: [0001, 0100, 1000]
(defn separate-bitboard [bitboard]
    (loop [n bitboard
           list-of-bitboards []]
        (let [remove-rightmost-setbit (bit-and n (dec n))]
            (if (zero? n)
                list-of-bitboards
                (recur remove-rightmost-setbit
                       (conj list-of-bitboards
                             (bit-xor n remove-rightmost-setbit)))))))

; bitwise-and with the boundary of the board so that we don't have to deal with negative number arithmetic
(defn get-valid-moves-bitmask [bitboards]
    (bit-and board-area ; to ensure moves aren't placed out of bounds
             (bit-not (reduce bit-or bitboards))))

; apply-move is idempotent; a move on an already-occupied square or out of bounds
; should return the same state
(defn apply-move [bitboards player move]
    ; Check we aren't placing on an already occupied square
    (if (> (bit-and (get-valid-moves-bitmask bitboards) move) 0)
        (assoc bitboards
               player
               (bit-and board-area ; to ensure moves aren't placed out of bounds
                        (bit-or (nth bitboards player) move)))
        bitboards))

(defn apply-move-to-state [{:keys [board current-player]} move]
    {:board          (apply-move board current-player move)
     :current-player (mod (inc current-player) (count board))})

(defn is-full [bitboards]
    (zero? (bit-xor board-area
                  (reduce bit-or bitboards))))

;  output |        meaning
; --------+------------------------
;   nil   | neither player has won
;    0    | player 1 wins
;    1    | player 2 wins
(defn check-win [bitboards]
    (loop [player 0
           win    nil]
        (if (< player (count bitboards))
            (if (some (fn [win-direction]
                          (= win-direction
                             (bit-and (nth bitboards player) win-direction)))
                      three-in-a-row)
                (recur (inc player) player)
                (recur (inc player) win))
            win)))

(defn is-terminal? [bitboards]
    (or (is-full bitboards)
        (nat-int? (check-win bitboards))))

(defn get-valid-moves-list [bitboards]
    (let [valid-moves-bitmask (get-valid-moves-bitmask bitboards)]
        (when (and (pos-int? valid-moves-bitmask) (not (is-terminal? bitboards)))
            (separate-bitboard valid-moves-bitmask))))


(defn make-random-agent [get-valid-moves-list]
    (fn [{:keys [board current-player]}]
        (rand-nth (get-valid-moves-list board))))

; Computation budget = number of iterations to run MCTS for
(defn configure-mcts-agent [player-index exploration computation-budget]
    (make-mcts-agent exploration
                     get-valid-moves-list
                     is-terminal?
                     apply-move-to-state
                     check-win
                     player-index
                     computation-budget))


; Lots of visual information to help debugging
(defn play-game [agents initial-board]
    (loop [history [initial-board]]
        (let [turn-number       (dec (count history))
              current-player    (rem turn-number (count agents))
              current-bitboards (nth history turn-number)
              current-state     {:board current-bitboards :current-player current-player}
              move              ((nth agents current-player) current-state)
              new-bitboards     (apply-move current-bitboards current-player move)
              new-history       (conj history new-bitboards)
              win-status        (check-win new-bitboards)]
            (println (format "Turn #%d" turn-number))
            (println (format "Current player: %d" current-player))
            (println "New board:")
            (print-game-state new-bitboards)
            (newline)
            (when (nat-int? win-status) (println "Player" win-status "wins!"))
            (newline)
            (print-board board-size width new-bitboards)
            (newline)
            (if (is-terminal? new-bitboards)
                new-history
                (recur new-history)))))

; No visual information, used for simulating a large amount of games
(defn play-game-result [agents initial-board]
    (loop [history [initial-board]]
        (let [turn-number       (dec (count history))
              current-player    (rem turn-number (count agents))
              current-bitboards (nth history turn-number)
              current-state     {:board current-bitboards :current-player current-player}
              move              ((nth agents current-player) current-state)
              new-bitboards     (apply-move current-bitboards current-player move)
              new-history       (conj history new-bitboards)
              win-status        (check-win new-bitboards)]
            (if (is-terminal? new-bitboards)
                win-status
                (recur new-history)))))


(defn bitboards-to-string [bitboards]
    (str "["
         (join " "
               (map (fn [bitboard] (cl-format nil
                                              (str "~" board-size ",'0B")
                                              bitboard))
               bitboards))
         "]"))

(defn history-to-string [history]
    (map bitboards-to-string history))

(defn play-game-print-history []
    (let [agents          [(configure-mcts-agent 0 1.2 100)
                           (configure-mcts-agent 1 1.2 1000)]
          history         (play-game agents new-game)
          history-strings (history-to-string history)]
        (run! println history-strings)))


(defn play-n-games [agents iterations]
    (loop [stats {:wins   [0 0]
                  :draws  0}
           n      iterations]
        (let [result (play-game-result agents new-game)]
            (if (zero? n)
                (println stats)
                (recur (cond (nil? result)
                                 (assoc stats :draws (inc (:draws stats)))
                             :else
                                 (assoc-in stats
                                           [:wins result]
                                           (inc (nth (:wins stats) result))))
                       (dec n))))))


(defn -main [& args]
    (println "Testing for 1000 games:")
    (println "Player 0 - random moves")
    (println "Player 1 - mcts-agent: exploration 1.2 iterations 1000")
    (println (play-n-games [(make-random-agent get-valid-moves-list)
                            (configure-mcts-agent 1 1.2 1000)]
                           1000))

    (println "Playing a game and printing visual info:")
    (play-game-print-history))
