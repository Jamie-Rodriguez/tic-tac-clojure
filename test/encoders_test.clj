(ns encoders-test
    (:use clojure.test)
    (:use encoders))

(deftest one-d-to-2-d-test
    (is (= [0 1] (one-d-to-2-d 1 3)))
    (is (= [1 1] (one-d-to-2-d 4 3)))
    (is (= [2 2] (one-d-to-2-d 8 3))))

(deftest state-to-one-plane-encoding-test
    (is (= [[0  0 0]
            [0 -1 0]
            [0  0 1]]
           (state-to-one-plane-encoding {:board [2r100000000 2r000010000]
                                         :player-to-move 0})))
    (is (= [[0  0 0]
            [0 -1 0]
            [0  0 1]]
           (state-to-one-plane-encoding {:board [2r000010000 2r100000000]
                                         :player-to-move 1}))))

(deftest one-hot-encode-move-test
    (is (= [[0 0 0]
            [0 0 1]
            [0 0 0]]
           (one-hot-encode-move 2r000100000))))
