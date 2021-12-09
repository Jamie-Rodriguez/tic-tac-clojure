(ns print
    (:use [clojure.pprint :only (cl-format)])
    (:use constants))


(defn print-game-state [bitboards]
    (doseq [[i player-bitboard] (map-indexed vector bitboards)]
        (println (str (format "player %d: " i)
                      (cl-format nil
                                 (str "~" board-size ",'0B")
                                 player-bitboard)))))


; Returns the player that occupies the square at index
; If no player occupies the square at index, return -1
; If a collision is present i.e. if for some reason multiple player's bitboards
; occupy the same square, use the first player found
(defn square-owner [index board]
    (loop [i      0
           owner -1]
        (if (or (not= owner -1)
                (>= i (count board)))
            owner
            (recur (inc i)
                   (if (pos? (bit-and (bit-shift-left 1 index)
                                      (nth board i)))
                       i
                       owner)))))

; Converts the game state (list of bitboards for each player)
; to a string of characters representing the board
; e.g. (game-state-to-string 9 ["O" "X"] [2r110001010 2r001010101])
; returns "XOXOX-XOO" (read left to right)
(defn game-state-to-string [board-size player-piece-symbols board]
    (loop [i      0
           result ""]
        (if (>= i board-size)
            result
            (recur (inc i)
                   (str result
                        (if (>= (square-owner i board) 0)
                            (nth player-piece-symbols
                                 (square-owner i board))
                            "-"))))))


; Used to calculate how much padding is required on numbers
; when printing the board to the console.
; This actually fails for n=1000 (returns 2 instead of 3),
; due to rounding error.
; But I don't expect anyone to create a board with 1000 squares...
(defn log-10 [n]
    (int (quot (Math/log n) (Math/log 10))))

(def num-extra-padding (log-10 board-size))


(defn create-dash-string [n]
    (loop [dash-string (str "-")
           i           (dec n)]
        (if (zero? i)
            dash-string
            (recur (str dash-string "-") (dec i)))))

(defn create-row-separator [n-col dash-size]
    (let [dash-string (create-dash-string dash-size)]
        (loop [row-separator ""
               i             (dec n-col)]
            (if (zero? i)
                (str row-separator dash-string)
                (recur (str row-separator dash-string "+")
                       (dec i))))))

(def row-separator
    (create-row-separator (quot board-size width)
                          (+ num-extra-padding 3)))


; Uses a chess-based coordinate system,
; i.e. print the rows in reverse order
; e.g. for a standard tic-tac-toe board, coordinates look like:
;  6 | 7 | 8
; ---+---+---
;  3 | 4 | 5
; ---+---+---
;  0 | 1 | 2
; Warning: I haven't tested this for other board dimensions
(defn print-board [board-size width bitboards]
    (let [n-rows       (quot (dec board-size)
                             width)
          board-string (game-state-to-string board-size
                                             player-piece-symbols
                                             bitboards)]
        (doseq [row (reverse (range (inc n-rows)))]
            (doseq [i (range (* row width) (+ (* row width) width))]
                (let [current-piece (nth board-string i)]
                    (print
                        (if (= i (* row width))
                            (str " " current-piece)
                            (str " | " current-piece)))))
            (newline)
            (when (pos? row) (println row-separator)))))
