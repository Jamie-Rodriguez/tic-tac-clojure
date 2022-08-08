(ns constants)

(def board-size 9)
(def width 3)

(def player-piece-symbols ["O" "X"])

(def board-area (dec (bit-shift-left 1 board-size)))

; TO-DO: find a way to generate this dynamically for arbitrary board dimensions
(def three-in-a-row [2r001001001
                     2r010010010
                     2r100100100
                     2r000000111
                     2r000111000
                     2r111000000
                     2r100010001
                     2r001010100])

(def new-game [2r0 2r0])