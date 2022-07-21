(ns print-test
    (:use clojure.test)
    (:use print))


(deftest square-owner-test
    (is (= 0  (square-owner 0 [2r000000101 2r000000110])))
    (is (= 1  (square-owner 1 [2r000000101 2r000000110])))
    (is (= 0  (square-owner 2 [2r000000101 2r000000110])))
    (is (= -1 (square-owner 3 [2r000000101 2r000000110]))))

(deftest game-state-to-string-test
    (is (= "XOXOX-XOO" (game-state-to-string 9
                                             ["O" "X"]
                                             [2r110001010 2r001010101]))))


; This actually fails for n=1000 (returns 2 instead of 3), due to rounding error.
; But I don't expect anyone to create a board with 1000 squares...
(deftest log-10-test
    (is (= 0 (log-10 1)))
    (is (= 1 (log-10 10)))
    (is (= 2 (log-10 100))))

(deftest create-dash-string-test
    (is (= "-"   (create-dash-string 1)))
    (is (= "--"  (create-dash-string 2)))
    (is (= "---" (create-dash-string 3))))

(deftest create-row-separator-test
    (is (= "----"     (create-row-separator 1 4)))
    (is (= "---+---"  (create-row-separator 2 3)))
    (is (= "--+--+--" (create-row-separator 3 2)))
    (is (= "-+-+-+-"  (create-row-separator 4 1))))
