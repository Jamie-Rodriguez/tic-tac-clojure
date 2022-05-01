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
    (is (= 0
           (get-valid-moves-bitmask [2r000011111 2r111100000])))
    (is (= 2r000000011
           (get-valid-moves-bitmask [2r000111100 2r111000000]))))

(deftest get-valid-moves-list-test
    (is (= nil
           (get-valid-moves-list [2r000011111 2r111100000])))
    (is (= [0x01 0x02]
           (get-valid-moves-list [2r000111100 2r111000000]))))

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
    (is (= false (is-full [2r001001001 0x00])))
    (is (= true  (is-full [2r001001001 2r110110110]))))

(deftest check-win-test
    (is (= 0   (check-win [2r001001001        0x00])))
    (is (= 1   (check-win [0x00        2r111000000])))
    (is (= nil (check-win [2r000001001 2r000000110]))))
