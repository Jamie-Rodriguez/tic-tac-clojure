(ns engine-test
    (:use clojure.test)
    (:use engine))

(deftest separate-bitboard-test
    (is (= [2r000000010
            2r000010000
            2r000100000
            2r010000000]
           (separate-bitboard 2r010110010))))

(deftest get-valid-moves-bitmask-test
    (is (= 0 (get-valid-moves-bitmask [2r000011111 2r111100000])))
    (is (= 2r000000011 (get-valid-moves-bitmask [2r000111100 2r111000000]))))

(deftest apply-move-test
    (is (= [1 0] (apply-move [0 0] 0 1)))
    (is (= [0 1] (apply-move [0 0] 1 1))))

; Should we reject the entire move, or only the out of bounds moves in the bitmask?
; Only rejecting the out of bounds moves for now
(deftest apply-move-out-of-bounds-test
    (is (= [0 1] (apply-move [0 0] 1 2r1000000001))))

(deftest apply-move-already-occupied-test
    (is (= [0 1] (apply-move [0 1] 0 1))))

(deftest apply-move-to-state-test
    (is (= {:board [2r100010010 2r011000101] :player-to-move 0}
           (apply-move-to-state {:board [2r100010010 2r011000100]
                                 :player-to-move 1}
                                2r000000001)))
    (is (= {:board [2r100011010 2r011000101] :player-to-move 1}
           (apply-move-to-state {:board [2r100010010 2r011000101]
                                 :player-to-move 0}
                                2r000001000))))

(deftest is-full-test
    (is (= false (is-full [2r001001001 0])))
    (is (= true  (is-full [2r001001001 2r110110110]))))

(deftest check-win-test
    (is (= 0   (check-win {:board [2r001001001           0]})))
    (is (= 1   (check-win {:board [0           2r111000000]})))
    (is (= nil (check-win {:board [2r000001001 2r000000110]}))))

(deftest is-terminal-test
    ; Win for player 1
    (is (= true (is-terminal? {:board [2r001001101 2r100010010]})))
    ; Draw: full board
    (is (= true (is-terminal? {:board [2r001110011 2r110001100]})))
    ; Non-terminal
    (is (= false (is-terminal? {:board [2r000000001 2r000000000]}))))

(deftest get-valid-moves-list-test
    (is (= [] (get-valid-moves-list {:board [2r000011111 2r111100000]})))
    (is (= [2r000000010 2r000000100]
           (get-valid-moves-list {:board [2r010110001 2r101001000]})))
    ; Terminal states should return empty list
    (is (= [] (get-valid-moves-list {:board [2r001100001 2r010010010]}))))
