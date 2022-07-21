(ns engine
    (:gen-class)
    (:use [clojure.string :only (join)])
    (:use [clojure.pprint :only (cl-format)])
    (:use constants)
    (:use print))


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

(defn get-valid-moves-list [bitboards]
    (let [valid-moves-bitmask (get-valid-moves-bitmask bitboards)]
        (when (pos-int? valid-moves-bitmask)
              (separate-bitboard valid-moves-bitmask))))

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


(defn make-random-agent [get-valid-moves-list]
    (fn [{:keys [board current-player]}]
        (rand-nth (get-valid-moves-list board))))


(defn play-game [agents initial-board]
    (loop [history [initial-board]]
        ; Should really move num-players binding outside of this loop to stop
        ; strictly unnecessary recalculations. But creating further nesting due
        ; to another 'let' form seems excessive just for this.
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


(defn -main [& args]
    (let [bitboards-to-string (fn [bitboards]
                                  (str "["
                                       (join " "
                                           (map (fn [bitboard]
                                                    (cl-format nil
                                                               (str "~" board-size ",'0B")
                                                               bitboard))
                                           bitboards))
                                       "]"))
          history-to-string   (fn [history] (map bitboards-to-string history))
          agents              [(make-random-agent get-valid-moves-list)
                               (make-random-agent get-valid-moves-list)]
          history             (play-game agents new-game)
          history-strings     (history-to-string history)]
        (run! println history-strings)))
