(ns engine
    (:use constants)
    (:use print))


; bitwise-and with the boundary of the board so that we don't have to deal with negative number arithmetic
(defn get-valid-moves [board]
    (bit-and board-area ; to ensure moves aren't placed out of bounds
             (bit-not (reduce bit-or board))))

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
             (if (= n 0)
                 list-of-bitboards
                 (recur remove-rightmost-setbit
                        (conj list-of-bitboards
                              (bit-xor n remove-rightmost-setbit)))))))

(defn random-valid-move [valid-moves]
    (rand-nth valid-moves))

; apply-move is idempotent; a move on an already-occupied square or out of bounds
; should return the same state
(defn apply-move [board player move]
    ; Check we aren't placing on an already occupied square
    (if (> (bit-and (get-valid-moves board) move) 0)
        (assoc board
               player
               (bit-and board-area ; to ensure moves aren't placed out of bounds
                        (bit-or (nth board player) move)))
        board))

(defn is-full [board]
    (= 0 (bit-xor board-area
                  (reduce bit-or board))))

; TO-DO: Return an int representing if a player wins e.g.:
;  output |        meaning
; --------+------------------------
;    -1   | neither player has won
;     0   | player 1 wins
;     1   | player 2 wins
;
; TO-DO: Change signature to board -> int
; (checking win should have no dependence on player)
(defn check-for-win [board player]
    (loop [i (dec (count three-in-a-row))]
        (let [win-direction (nth three-in-a-row i)
              is-win        (= win-direction
                               (bit-and (nth board player) win-direction))]
            ; Checking is-win allows us to escape the loop early if a win is found
            (if (or is-win (= i 0))
                is-win
                (recur (dec i))))))


(defn play-game [move-generators
                 initial-board]
    (loop [history [initial-board]]
        ; Should really move num-players binding outside of this loop
        ; to stop strictly unnecessary recalculations. But creating further nesting
        ; due to another 'let' form seems excessive just for this.
        (let [num-players         (count move-generators)
              turn-number         (dec (count history))
              current-player      (rem turn-number num-players)
              current-board       (nth history turn-number)
              valid-moves-bitmask (get-valid-moves current-board)
              valid-moves-list    (separate-bitboard valid-moves-bitmask)
              move                ((nth move-generators current-player) valid-moves-list)
              new-board           (apply-move current-board current-player move)
              new-history         (conj history new-board)]
            (println (format "Turn #%d" turn-number))
            (println (format "Current player: %d" current-player))
            (println "New board:")
            (print-game-state new-board)
            (newline)
            (print-board board-size width new-board)
            (newline)
            (if (or (is-full new-board)
                    (check-for-win new-board current-player))
                new-history
                (recur new-history)))))


(defn -main []
    (println
        (play-game [random-valid-move
                    random-valid-move]
                   new-game)))
