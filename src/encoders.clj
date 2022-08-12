(ns encoders
    (:gen-class)
    (:use [print :only (square-owner)])
    (:require [constants :refer [board-size
                                 width]]))

(defn one-d-to-2-d [i board-width]
    [(int (Math/floor (/ i board-width)))
     (rem i board-width)])

(def one-plane-encoding { :not-current-player -1
                          :empty               0
                          :current-player      1 })

(defn state-to-one-plane-encoding [{:keys [board player-to-move]}]
    (loop [i        0
           encoded []]
        (if (>= i board-size)
            encoded
            (let [[r c]  (one-d-to-2-d i width)
                  square (square-owner i board)
                  value  (cond (= square player-to-move)
                                  (:current-player one-plane-encoding)
                               ; From square-owner, -1 = empty square
                               (not= square -1)
                                   (:not-current-player one-plane-encoding)
                               :else
                                   (:empty one-plane-encoding))]
                (recur (inc i)
                       (if (zero? (mod i width))
                           ; new row
                           (conj encoded [value])
                           ; update existing row (append new value)
                           (assoc encoded r (conj (nth encoded r) value))))))))

(defn one-hot-encode-move [move]
    (loop [i        0
           encoded []]
        (if (>= i board-size)
            encoded
            (let [[r c]  (one-d-to-2-d i width)
                  square (bit-and (bit-shift-right move i) 1)]
                (recur (inc i)
                       (if (zero? (mod i width))
                           ; new row
                           (conj encoded [square])
                           ; update existing row (append new value)
                           (assoc encoded r (conj (nth encoded r) square))))))))
