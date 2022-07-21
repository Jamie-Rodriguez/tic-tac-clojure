(ns mcts-test
    (:use clojure.test)
    (:use mcts))


(deftest uct-test
    (is (= 2.5 (uct 1                ; Exploration parameter
                    (Math/exp 8)     ; total rollouts on parent node
                    {:num-rollouts 2 ; Node statistics
                     :score 1})))
    (is (= ##Inf (uct 1                 ; Exploration parameter
                      (Math/exp 8)      ; total rollouts on parent node
                      {:num-rollouts 0  ; Node statistics
                       :score 0}))))

(deftest pick-best-move-test
    (is (= 0 (pick-best-move 1.5 {:moves [{:num-rollouts 2
                                           :score 1}]})))
    (is (= 0 (pick-best-move 1.5 {:moves [{:num-rollouts 2
                                           :score 1}
                                          {:num-rollouts 8
                                           :score 3}]})))
    (is (= 1 (pick-best-move 1.5 {:moves [{:num-rollouts 2
                                           :score 1}
                                          {:num-rollouts 0
                                           :score 0}]}))))

(deftest get-unexplored-moves-test
    (let [mock-get-valid-moves (fn [board] [2r001000000 2r100000000])]
        (is (= nil
               (get-unexplored-moves mock-get-valid-moves
                                     {:state {:board [2r010100101 2r000011010]
                                              :current-player 1}
                                      :move 2r000100000
                                      :num-rollouts 5
                                      :score 2
                                      :moves [{:move 2r001000000
                                               :state {:board [2r010100101 2r001011010]
                                                       :current-player 0}
                                               :num-rollouts 2
                                               :score 2
                                               :moves []}
                                              {:move 2r100000000
                                               :state {:board [2r010100101 2r100011010]
                                                       :current-player 0}
                                               :num-rollouts 1
                                               :score 0
                                               :moves []}]})))
        (is (= [2r100000000]
               (get-unexplored-moves mock-get-valid-moves
                                     {:state {:board [2r010100101 2r000011010]
                                              :current-player 1}
                                      :move 2r000100000
                                      :num-rollouts 5
                                      :score 5
                                      :moves [{:move 2r001000000
                                               :state {:board [2r010100101 2r001011010]
                                                       :current-player 0}
                                               :num-rollouts 5
                                               :score 5
                                               :moves []}]})))))

(deftest pick-unexplored-move-test
    (let [; "Random" number generator to pick a move from the valid moves
          mock-random-num  (fn [] 1)
          ; Valid/unexplored moves generator
          mock-valid-moves (fn [board] (cond
                                           (= board [2r100010010 2r011000100])
                                               [2r000100000 2r000001000]
                                           (= board [2r001110001 2r110001100])
                                               [2r000000010]
                                           :else
                                               []))
          is-terminal?     (fn [board] (= board [2r001001101 2r100010010]))]
        ; Not fully explored
        (is (= 2r000001000
               (pick-unexplored-move mock-random-num
                                     mock-valid-moves
                                     is-terminal?
                                     {:state {:board [2r100010010 2r011000100]
                                              :current-player 1}
                                      :move 2r100000000
                                      :num-rollouts 2
                                      :score 1
                                      :moves [{:move 2r000000001
                                               :state {:board [2r100010010 2r011000101]
                                                       :current-player 0}
                                               :num-rollouts 2
                                               :score 1
                                               :moves []}]})))
        ; Fully-explored
        ; Selection should never pick a fully-explored node...
        (is (= nil
               (pick-unexplored-move mock-random-num
                                     mock-valid-moves
                                     is-terminal?
                                     {:state {:board [2r001110001 2r110001100]
                                              :current-player 0}
                                      :move 2r010000000
                                      :num-rollouts 2
                                      :score 0
                                      :moves [{:move 2r000000010
                                               :state {:board [2r001110011 2r110001100]
                                                       :current-player 1}
                                               :num-rollouts 1
                                               :score 0
                                               :moves []}]})))
        ; Terminal state
        (is (= nil
               (pick-unexplored-move mock-random-num
                                     mock-valid-moves
                                     is-terminal?
                                     {:state {:board [2r001001101 2r100010010]
                                              :current-player 1}
                                      :move 2r000001000
                                      :num-rollouts 2
                                      :score 1
                                      :moves []})))))

(deftest select-test
    ; Start from state { :board [2r010000101 2r000011010] :current-player 0 }
    ; There are three possible moves to choose from.
    (let [initial-state      {:state {:board [2r010000101 2r000011010]
                                      :current-player 0}
                              :move 2r000001000
                              :num-rollouts 5
                              :score 3
                              :moves []}
          exploration        1.5
          mock-tie-breaker   (fn [] 1)
          ; Need to have two different outputs as the last test checks that
          ; selection drills further down the tree.
          ; Therefore also need to make sure that we can output the valid moves
          ; of the next state
          mock-valid-moves   (fn [board] (if (= board [2r010000101 2r000011010])
                                             [2r000100000 2r001000000 2r100000000]
                                             [2r000100000 2r100000000]))
          mock-is-terminal?  (fn [board] false)]
        ; Test case: no moves have been expanded for root
        ; Select root node for expansion
        (is (= [] (select exploration
                          mock-tie-breaker
                          mock-valid-moves
                          mock-is-terminal?
                          initial-state)))
        ; Test case: only one move out of three has been expanded for root
        ; Select root node for expansion
        (is (= []
               (select exploration
                       mock-tie-breaker
                       mock-valid-moves
                       mock-is-terminal?
                       (assoc initial-state
                              :moves
                              [{:move 2r000100000
                                :state {:board [2r010100101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 5
                                :score 3
                                :moves []}]))))
        ; Test case: all three moves have been expanded for root,
        ; with same statistics
        ; Resolve tie-break for selection
        (is (= [2r001000000]
               (select exploration
                       mock-tie-breaker
                       mock-valid-moves
                       mock-is-terminal?
                       (assoc initial-state
                              :moves
                              [{:move 2r000100000
                                :state {:board [2r010100101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 1
                                :score 1
                                :moves []}
                               {:move 2r001000000
                                :state {:board [2r011000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 1
                                :score 1
                                :moves []}
                               {:move 2r100000000
                                :state {:board [2r110000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 1
                                :score 1
                                :moves []}]))))
        ; Test case: all three moves have been expanded for root,
        ; with differing statistics
        ; Select move with highest UCT value
        (is (= [2r001000000]
               (select exploration
                       mock-tie-breaker
                       mock-valid-moves
                       mock-is-terminal?
                       (assoc initial-state
                              :moves
                              [{:move 2r000100000
                                :state {:board [2r010100101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 1
                                :score 0
                                :moves []}
                               {:move 2r001000000
                                :state {:board [2r011000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 2
                                :score 2
                                :moves []}
                               {:move 2r100000000
                                :state {:board [2r110000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 2
                                :score 1
                                :moves []}]))))
        ; Test case: all three moves have been expanded for root,
        ; with differing statistics
        ; Select move with highest UCT value - which is fully expanded.
        ; (i.e. not a leaf node)
        ; Select *non-terminal* child node
        (is (= [2r001000000 2r000100000]
               (select exploration
                       mock-tie-breaker
                       mock-valid-moves
                       mock-is-terminal?
                       (assoc initial-state
                              :moves
                              [{:move 2r000100000
                                :state {:board [2r010100101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 1
                                :score 0
                                :moves []}
                               {:move 2r001000000
                                :state {:board [2r011000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 2
                                :score 2
                                ; Make stats of these two moves identical, to
                                ; check that select() intellegently picks the
                                ; non-terminal state.
                                ; (The tie-breaker would otherwise
                                ; *incorrectly* pick the move at index 1)
                                :moves [{:move 2r100000000
                                         :state {:board [2r011000101 2r100011010]
                                                 :current-player 0}
                                         :num-rollouts 1
                                         :score 0
                                         :moves []}
                                        ; This is a terminal state.
                                        ; We should not be choosing terminal
                                        ; states for selection
                                        {:move 2r000100000
                                         :state {:board [2r011000101 2r000111010]
                                                 :current-player 0}
                                         :num-rollouts 1
                                         :score 0
                                         :moves []}]}
                               {:move 2r100000000
                                :state {:board [2r110000101 2r000011010]
                                        :current-player 1}
                                :num-rollouts 2
                                :score 1
                                :moves []}]))))
        ; Test with a tree that causes select() to choose a terminal state (draw)
        (is (= [2r000001000 2r000100000]
               (select 1
                       (fn [] 1)
                       (fn [board] (cond (= board [2r101010010 2r010000101])
                                            [2r000001000 2r000100000]
                                         (= board [2r101010010 2r010001101])
                                            [2r000100000]
                                         :else
                                            []))
                       (fn [board] (or (= board [2r101011010 2r010100101])
                                       (= board [2r101110010 2r010001101])))
                       {:state {:board [2r101010010 2r010000101]
                                :current-player 1}
                        :num-rollouts 4
                        :score 0
                        :moves [{:state {:board [2r101010010 2r010100101]
                                         :current-player 0}
                                 :move 2r000100000
                                 :num-rollouts 2
                                 :score 0
                                 :moves [{:state {:board [2r101011010 2r010100101]
                                                  :current-player 1}
                                          :move 2r000001000
                                          :num-rollouts 1
                                          :score 0
                                          :moves []}]}
                                {:state {:board [2r101010010 2r010001101]
                                                 :current-player 0}
                                 :move 2r000001000
                                 :num-rollouts 2
                                 :score 0
                                 :moves [{:state {:board [2r101110010 2r010001101]
                                                  :current-player 1}
                                          :move 2r000100000
                                          :num-rollouts 1
                                          :score 0
                                          :moves []}]}]})))
        ; TO-DO: Write test with a tree that causes select() to choose a terminal state
        ; where there is a win BUT the board is NOT empty
        ; select() should not drill further down!
))

(deftest treewalk-test
    (is (= {:move 2r000000010
            :state {:board [2r000000001 2r000000010]
                    :current-player 0}
            :num-rollouts 2
            :score 1
            :moves [{:move 2r000000100
                     :state {:board [2r000000101 2r000000010]
                             :current-player 1}
                     :num-rollouts 1
                     :score 1
                     :moves []}]}
           (treewalk [2r000000001 2r000000010]
                     {:state {:board [2r000000000 2r000000000]
                              :current-player 0}
                      :move 2r000000000
                      :num-rollouts 4
                      :score 3
                      :moves [{:move 2r000000001
                              :state {:board [2r000000001 2r000000000]
                                      :current-player 1}
                              :num-rollouts 1
                              :score 1
                              :moves [{:move 2r000000010
                                       :state {:board [2r000000001 2r000000010]
                                               :current-player 0}
                                       :num-rollouts 2
                                       :score 1
                                       :moves [{:move 2r000000100
                                                :state {:board [2r000000101 2r000000010]
                                                        :current-player 1}
                                                :num-rollouts 1
                                                :score 1
                                                :moves []}]}]}]})))
    (is (= {:state {:board [2r000000000 2r000000000]
                    :current-player 0}
            :move 2r000000000
            :num-rollouts 4
            :score 3
            :moves [{:move 2r000000001
                     :state {:board [2r000000001 2r000000000]
                             :current-player 1}
                     :num-rollouts 1
                     :score 1
                     :moves []}]}
           (treewalk []
                     {:state {:board [2r000000000 2r000000000]
                              :current-player 0}
                      :move 2r000000000
                      :num-rollouts 4
                      :score 3
                      :moves [{:move 2r000000001
                               :state {:board [2r000000001 2r000000000]
                                       :current-player 1}
                               :num-rollouts 1
                               :score 1
                               :moves []}]}))))

(deftest update-test
    (is (= {:state {:board [2r000000000 2r000000000]
                    :current-player 0}
            :move 2r000000000
            :num-rollouts 4
            :score 3
            :moves [{:move 2r000000001
                     :state {:board [2r000000001 2r000000000]
                             :current-player 1}
                     :num-rollouts 1
                     :score 1
                     :moves [{:move 2r000000010
                              :state {:board [2r000000001 2r000000010]
                                      :current-player 0}
                              :num-rollouts 2
                              :score 1
                              :moves [{:move 2r000000100
                                       :state {:board [2r000000101 2r000000010]
                                               :current-player 1}
                                       :num-rollouts 1
                                       :score 1
                                       :moves []}
                                      {:move 2r000001000
                                       :state {:board [2r000001001 2r000000010]
                                               :current-player 1}
                                       :num-rollouts 1
                                       :score 1
                                       :moves []}]}]}]}
           (update-node {:state {:board [2r000000000 2r000000000]
                                 :current-player 0}
                         :move 2r000000000
                         :num-rollouts 4
                         :score 3
                         :moves [{:move 2r000000001
                                  :state {:board [2r000000001 2r000000000]
                                          :current-player 1}
                                  :num-rollouts 1
                                  :score 1
                                  :moves [{:move 2r000000010
                                           :state {:board [2r000000001 2r000000010]
                                                   :current-player 0}
                                           :num-rollouts 2
                                           :score 1
                                           :moves [{:move 2r000000100
                                                    :state {:board [2r000000101 2r000000010]
                                                            :current-player 1}
                                                    :num-rollouts 1
                                                    :score 1
                                                    :moves []}]}]}]}
                        [2r000000001 2r000000010]
                        {:move 2r000000010
                         :state {:board [2r000000001 2r000000010]
                                 :current-player 0}
                         :num-rollouts 2
                         :score 1
                         :moves [{:move 2r000000100
                                  :state {:board [2r000000101 2r000000010]
                                          :current-player 1}
                                  :num-rollouts 1
                                  :score 1
                                  :moves []}
                                 {:move 2r000001000
                                  :state {:board [2r000001001 2r000000010]
                                          :current-player 1}
                                  :num-rollouts 1
                                  :score 1
                                  :moves []}]}))))

(deftest simulate-test
    (let [mock-is-terminal?        (fn [board] (reduce
                                                   (fn [found-match? terminal-state]
                                                       (or found-match? (= board terminal-state)))
                                                   false
                                                   [[2r110100101 2r001011010]
                                                    [2r011100101 2r100011010]
                                                    [2r011000101 2r000111010]
                                                    [2r110000101 2r000111010]]))
          mock-check-win           (fn [board] (cond (= board [2r110100101 2r001011010])
                                                         0
                                                     (= board [2r011100101 2r100011010])
                                                         nil
                                                     (or (= board [2r110000101 2r000111010])
                                                         (= board [2r011000101 2r000111010]))
                                                         1
                                                     :else
                                                         "SHOULD NOT HAVE HIT HERE"))
          mock-valid-moves         (fn [board] (cond (= board [2r010100101 2r000011010])
                                                         [2r001000000 2r100000000]
                                                     (= board [2r011000101 2r000011010])
                                                         [2r000100000 2r100000000]
                                                     (= board [2r110000101 2r000011010])
                                                         [2r000100000 2r001000000]
                                                     (= board [2r010100101 2r001011010])
                                                         [2r100000000]
                                                     (= board [2r010100101 2r100011010])
                                                         [2r001000000]
                                                     (= board [2r011000101 2r100011010])
                                                         [2r000100000]
                                                     (= board [2r110000101 2r001011010])
                                                         [2r000100000]
                                                     :else
                                                         "SHOULD NOT HAVE HIT HERE"))
          pseudo-random            (fn [seed]
                                       (let [gen (java.util.Random. seed)]
                                           (fn [] (Math/abs (.nextInt gen)))))
          p-random-gen             (pseudo-random 1234)
          mock-apply-move-to-state (fn [state move]
                                       (cond
                                           ; 2-ply states
                                           (= state
                                              {:board [2r010100101 2r000011010]
                                               :current-player 1})
                                              (if (= move 2r001000000)
                                                  {:board [2r010100101 2r001011010]
                                                   :current-player 0}
                                                  ; 2r100000000
                                                  {:board [2r010100101 2r100011010]
                                                   :current-player 0})
                                           (= state
                                              {:board [2r011000101 2r000011010]
                                               :current-player 1})
                                              (if (= move 2r000100000)
                                                  {:board [2r011000101 2r000111010]
                                                   :current-player 0}
                                                  ; 2r100000000
                                                  {:board [2r011000101 2r100011010]
                                                   :current-player 0})
                                           (= state
                                              {:board [2r110000101 2r000011010]
                                               :current-player 1})
                                              (if (= move 2r000100000)
                                                  {:board [2r110000101 2r000111010]
                                                   :current-player 0}
                                                  ; 2r001000000
                                                  {:board [2r110000101 2r001011010]
                                                   :current-player 0})
                                           ; 1-ply states
                                           (= state
                                              {:board [2r010100101 2r001011010]
                                               :current-player 0})
                                              {:board [2r110100101 2r001011010]
                                               :current-player 1}
                                           (= state
                                              {:board [2r010100101 2r100011010]
                                               :current-player 0})
                                              {:board [2r110100101 2r001011010]
                                               :current-player 1}
                                           (= state
                                              {:board [2r011000101 2r100011010]
                                               :current-player 0})
                                              {:board [2r011100101 2r100011010]
                                               :current-player 1}
                                           (= state
                                              {:board [2r110000101 2r001011010]
                                               :current-player 0})
                                              {:board [2r110100101 2r001011010]
                                               :current-player 1}
                                           :else
                                               "SHOULD NOT HAVE HIT HERE"))]
        ; 2-ply moves
        ; -----------
        ; Win or draw possible
        (is (.contains [nil 0]
                       (simulate mock-is-terminal?
                                 mock-check-win
                                 mock-valid-moves
                                 p-random-gen
                                 mock-apply-move-to-state
                                 {:board [2r010100101 2r000011010] :current-player 1})))
        ; Lose or draw possible
        (is (.contains [nil 1]
                       (simulate mock-is-terminal?
                                 mock-check-win
                                 mock-valid-moves
                                 p-random-gen
                                 mock-apply-move-to-state
                                 {:board [2r011000101 2r000011010] :current-player 1})))
        ; Win or lose possible
        (is (.contains [0 1]
                       (simulate mock-is-terminal?
                                 mock-check-win
                                 mock-valid-moves
                                 p-random-gen
                                 mock-apply-move-to-state
                                 {:board [2r110000101 2r000011010] :current-player 1})))
        ; Terminal states
        ; ---------------
        ; Terminal state: win player 1
        (is (= 0 (simulate mock-is-terminal?
                           mock-check-win
                           mock-valid-moves
                           p-random-gen
                           mock-apply-move-to-state
                           {:board [2r110100101 2r001011010] :current-player 1})))
        ; Terminal state: draw
        (is (= nil (simulate mock-is-terminal?
                             mock-check-win
                             mock-valid-moves
                             p-random-gen
                             mock-apply-move-to-state
                             {:board [2r011100101 2r100011010] :current-player 1})))
        ; Terminal state: lose (player 2 wins)
        (is (= 1 (simulate mock-is-terminal?
                           mock-check-win
                           mock-valid-moves
                           p-random-gen
                           mock-apply-move-to-state
                           {:board [2r011000101 2r000111010] :current-player 0})))))

(deftest is-path-valid-test
    (let [tree {:move 2r000001000
                :state {:board [2r010000101 2r000011010]
                        :current-player 0}
                :num-rollouts 4
                :score 2
                :moves [{:move 2r100000000
                         :state {:board [2r110000101 2r000011010]
                                 :current-player 1}
                         :num-rollouts 2
                         :score 1
                         :moves [{:move 2r001000000
                                  :state {:board [2r110000101 2r001011010]
                                          :current-player 0}
                                  :num-rollouts 0
                                  :score 0
                                  :moves []}]}]}]
        (is (= true (is-path-valid? tree [2r100000000 2r001000000])))
        (is (= false (is-path-valid? tree [2r100000000 2r000000001])))
        (is (= false (is-path-valid? tree [2r100000000 2r001000000 2r000000001])))
        (is (= true (is-path-valid? tree [])))))

; Not testing if an invalid path is passed in
; for now it will throw a null pointer exception
(deftest backprop-test
    (is (= {:move 2r000001000
            :state {:board [2r010000101 2r000011010]
                    :current-player 0}
            :num-rollouts 5
            :score 3
            :moves [{:move 2r001000000
                     :state {:board [2r011000101 2r000011010]
                             :current-player 1}
                     :num-rollouts 1
                     :score 0
                     :moves []}
                    {:move 2r100000000
                     :state {:board [2r110000101 2r000011010]
                             :current-player 1}
                     :num-rollouts 3
                     :score 2
                     :moves [{:move 2r000100000
                              :state {:board [2r110000101 2r000111010]
                                      :current-player 0}
                              :num-rollouts 1
                              :score 0
                              :moves []}
                              ; This is the leaf node to backprop from
                             {:move 2r001000000
                              :state {:board [2r110000101 2r001011010]
                                      :current-player 0}
                              :num-rollouts 1
                              :score 1
                              :moves []}]}]}
           (backprop 1
                     [2r100000000 2r001000000]
                     {:move 2r000001000
                      :state {:board [2r010000101 2r000011010]
                              :current-player 0}
                      :num-rollouts 4
                      :score 2
                      :moves [{:move 2r001000000
                              :state {:board [2r011000101 2r000011010]
                                      :current-player 1}
                              :num-rollouts 1
                              :score 0
                              :moves []}
                              {:move 2r100000000
                              :state {:board [2r110000101 2r000011010]
                                      :current-player 1}
                              :num-rollouts 2
                              :score 1
                              :moves [{:move 2r000100000
                                       :state {:board [2r110000101 2r000111010]
                                               :current-player 0}
                                       :num-rollouts 1
                                       :score 0
                                       :moves []}
                                      ; This is the leaf node to backprop from
                                      {:move 2r001000000
                                       :state {:board [2r110000101 2r001011010]
                                               :current-player 0}
                                       :num-rollouts 0
                                       :score 0
                                       :moves []}]}]}))))
