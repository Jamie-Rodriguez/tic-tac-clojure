(ns engine-test
    (:use clojure.test)
    (:use engine))


(deftest get-valid-moves-test
    (is (= 2r111111100
           (get-valid-moves [0x01 0x02]))))

(deftest separate-bitboard-test
    (is (= [2r00000010
            2r00010000
            2r00100000
            2r10000000]
           (separate-bitboard 2r10110010))))

(deftest apply-move-test
    (is (= [0x01 0]
           (apply-move [2r0 2r0] 0 0x01)))
    (is (= [0 0x01]
           (apply-move [2r0 2r0] 1 0x01))))

; Should we reject the entire move, or only the out of bounds moves in the bitmask?
; Only rejecting the out of bounds moves for now
(deftest apply-move-out-of-bounds-test
    (is (= [0 0x01]
           (apply-move [0 0] 1 2r1000000001))))

(deftest apply-move-already-occupied-test
    (is (= [0 0x01]
           (apply-move [0 0x01] 0 0x01))))

(deftest is-full-test
    (is (= false  (is-full [2r001001001 0x00])))
    (is (= true   (is-full [2r001001001 2r110110110]))))

(deftest check-for-win-test
    (is (= true  (check-for-win [2r001001001 0x00]        0)))
    (is (= true  (check-for-win [0x00        2r111000000] 1)))
    (is (= false (check-for-win [2r000001001 2r000000110] 0))))
