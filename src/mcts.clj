(ns mcts
    (:gen-class))

; Tree node schema:
; state
; num-rollouts
; score
; moves

; Although you could calculate the node statistics by populating them back up
; recursively from the leaf nodes, for a large tree this will be quite
; expensive. So calculate and cache the statistics, updating only with new leaf
; data as we see it.
; Cache the values of num-rollouts and score to prevent repeated computations


(defn uct [exploration total-rollouts-parent {:keys [num-rollouts score]}]
    "Upper Confidence Bound 1 applied to trees,
     from Levente Kocsis and Csaba Szepesvári"
    (if (pos-int? num-rollouts)
        (+ (/ score num-rollouts)
           (* exploration
              (Math/sqrt (/ (Math/log total-rollouts-parent) num-rollouts))))
        ##Inf))

(defn pick-best-move [exploration node]
    (let [moves                 (:moves node)
          total-rollouts-parent (reduce (fn [sum child]
                                            (+ sum (:num-rollouts child)))
                                        0
                                        moves)
          get-uct               (partial uct exploration total-rollouts-parent)]
        ; We need to use 'loop' because we need to return the index,
        ; but Clojure's 'reduce' doesn't keep track of the index >:^(
        (loop [current-node  0
               best-uct-node 0]
            (if (< current-node (count moves))
                (if (> (get-uct (nth moves current-node))
                       (get-uct (nth moves best-uct-node)))
                    (recur (inc current-node) current-node)
                    (recur (inc current-node) best-uct-node))
                best-uct-node))))

(defn get-unexplored-moves [get-valid-moves {:keys [state moves]}]
    (let [valid-moves    (get-valid-moves (state :board))
          explored-moves (map (fn [move] (move :move)) moves)]
        (not-empty (filter (fn [move] (not (.contains explored-moves move)))
                           valid-moves))))

(defn pick-unexplored-move [get-random-int get-valid-moves is-terminal? {:keys [state moves]}]
    (when-not (is-terminal? (:board state))
        (let [valid-moves    (get-valid-moves (state :board))
              explored-moves (map (fn [move] (move :move)) moves)]
            (when-let [unexplored-moves (not-empty (filter (fn [move]
                                                           (not (.contains explored-moves move)))
                                                           valid-moves))]
                (nth unexplored-moves (mod (get-random-int) (count unexplored-moves)))))))

(defn select [exploration get-random-int get-valid-moves is-terminal? tree]
    (loop [current-node tree
           path         []]
        ; If we have arrived at a not fully explored node or a terminal state
        (if (or (not= (count (get-valid-moves (get-in current-node [:state :board])))
                      (count (current-node :moves)))
                (is-terminal? (get-in current-node [:state :board])))
            path
            (let [get-uct-value (partial uct exploration (current-node :num-rollouts))
                  indexed-ucts (map-indexed (fn [i node] {:index i
                                                          :uct (get-uct-value node)})
                                            (current-node :moves))
                  max-ucts (reduce (fn [max-values curr]
                                       (cond (= (curr :uct) (:uct (first max-values)))
                                                (conj max-values curr)
                                             (> (curr :uct) (:uct (first max-values)))
                                                [curr]
                                             :else
                                                max-values))
                                   [(first indexed-ucts)]
                                   (rest indexed-ucts))
                  ; There is a possibility that multiple moves may have the same statistics,
                  ; giving the same UCT values.
                  ; Settle the tie-break
                  next-node (nth (current-node :moves)
                                 (:index (nth max-ucts
                                              (mod (get-random-int) (count max-ucts)))))]
                (recur next-node (conj path (next-node :move)))))))

; Probably don't need unit tests for this tiny helper function...
; Testing found using reduce()'s early escape 'reduced()' faster than a basic
; filter() method
(defn find-next-node [next-move nodes]
    (reduce (fn [dont-care node] (if (= (get node :move) next-move)
                                     (reduced node)
                                     nil))
            nil
            nodes))

(defn treewalk [path node]
    (loop [current-node node
           current-path path]
        (if (empty? current-path)
            current-node
            (recur (find-next-node (first current-path) (get current-node :moves))
                   (rest current-path)))))

; This is the "expansion" step, but I think "replace node" is more indicative
; of *how* we are achieving it
; TO-DO: Protect against invalid path
(defn replace-node [node path updated-node]
    (if (empty? path)
        updated-node
        (assoc node
               ; Note: It doesn't matter, but this conj() operation changes
               ; the original order of the moves
                            ; Have to convert back into a vector because
                            ; filter() returns a lazy sequence
               :moves (conj (vec (filter (fn [n] (not= (:move n) (first path)))
                                         (get node :moves)))
               (replace-node (find-next-node (first path) (get node :moves))
                             (rest path)
                             updated-node)))))

(defn simulate [is-terminal?
                check-win
                valid-moves
                random-int
                apply-move
                state]
    (loop [current-state state]
        (if (is-terminal? (:board current-state))
            (check-win (:board current-state))
            (let [moves     (valid-moves (:board current-state))
                  next-move (nth moves (mod (random-int) (count moves)))]
                (recur (apply-move current-state next-move))))))

(defn is-path-valid? [node path]
    (loop [current-node node
           current-path path]
        (if (empty? current-path)
            true
            (if-let [next-node (find-next-node (first current-path)
                                               (get current-node :moves))]
                (recur next-node (rest current-path))
                false))))

; Note: The root node's score is not actually used, but we backprop up to it
; and update it anyway
; Use a depth-first search style recursion to drill down the tree along 'path',
; returning updated nodes as the call-stack collapses back up the tree to the
; root node
; TO-DO: Change backprop to not update the score of the root node
; TO-DO: Protect against invalid path
; If not all the moves on 'path' exist in the tree, will throw a null pointer
; exception when trying to access the non-existent current node, which = null
(defn backprop [who-won? path node previous-player]
    (let [new-node (assoc node
                          :num-rollouts (inc (:num-rollouts node))
                          :score (+ (:score node)
                                    (cond (= who-won? previous-player)
                                              1
                                          (nil? who-won?)
                                              0
                                          :else
                                              -1)))]
        (if (empty? path)
            new-node
            (assoc new-node
                   ; Note: It doesn't matter, but this conj() operation changes
                   ; the original order of the moves
                                ; The other child moves
                                ; Have to convert back into a vector because
                                ; filter() returns a lazy sequence
                   :moves (conj (vec (filter (fn [n] (not= (:move n) (first path)))
                                             (get new-node :moves)))
                                ; Get the next move along the path with updated stats
                                (backprop who-won?
                                          (rest path)
                                          (find-next-node (first path) (get new-node :moves))
                                          (get-in node [:state :player-to-move])))))))

(defn make-mcts-agent [exploration
                       get-valid-moves
                       is-terminal?
                       apply-move
                       check-win
                       player-index
                       computation-budget]
    (fn [state]
        (loop [tree   {:state state
                       :num-rollouts 0
                       :score 0
                       :moves []}
               budget computation-budget]
            (if (> budget 0)
                (let [selected-node-path (select exploration
                                                 (fn [] (rand-int Integer/MAX_VALUE))
                                                 get-valid-moves
                                                 is-terminal?
                                                 tree)
                      selected-node (treewalk selected-node-path tree)
                      unexplored-move (pick-unexplored-move (fn [] (rand-int Integer/MAX_VALUE))
                                                            get-valid-moves
                                                            is-terminal?
                                                            selected-node)
                      ; If selection picks a terminal state, unexplored move will be nil.
                      ; Don't expand the selected node in this case (there is nothing to expand with!)
                      path (if unexplored-move
                               (conj selected-node-path unexplored-move)
                               selected-node-path)
                      new-tree (if unexplored-move
                                   (let [unexplored-node {:state        (apply-move (selected-node :state)
                                                                                    unexplored-move)
                                                          :move         unexplored-move
                                                          :num-rollouts 0
                                                          :score        0
                                                          :moves        []}
                                         expanded-node   (assoc-in selected-node
                                                                   [:moves]
                                                                   (conj (selected-node :moves)
                                                                         unexplored-node))]
                                        (replace-node tree
                                                      selected-node-path
                                                      expanded-node))
                                   tree)
                      ; Simulate handles terminal nodes
                      simulation-result (simulate is-terminal?
                                                  check-win
                                                  get-valid-moves
                                                  (fn [] (rand-int Integer/MAX_VALUE))
                                                  apply-move
                                                  (:state (treewalk path new-tree)))]
                    ; The root node's score is not actually used, but we
                    ; backprop up to it and update it anyway.
                    ; We don't know the previous state, especially for the case
                    ; that the root node is the start of the game i.e. there
                    ; was not previous state
                    (recur (backprop simulation-result
                                     path
                                     new-tree
                                     -1)
                           (dec budget)))
                ; https://ai.stackexchange.com/questions/16905/mcts-how-to-choose-the-final-action-from-the-root
                ; Choose best move via the "robust child" method = highest # of visits
                ; Tie-break strategy: random choice
                (let [max-rollouts (reduce (fn [most-visited node]
                                               (cond (= (node :num-rollouts) (:num-rollouts (first most-visited)))
                                                        (conj most-visited node)
                                                     (> (node :num-rollouts) (:num-rollouts (first most-visited)))
                                                        [node]
                                                     :else
                                                        most-visited))
                                           [(first (get tree :moves))]
                                           (rest (get tree :moves)))
                      ; There is a possibility that multiple moves may have the same statistics,
                      ; having the same number of rollouts.
                      ; Settle the tie-break
                      next-node (nth max-rollouts (mod (rand-int Integer/MAX_VALUE)
                                                       (count max-rollouts)))]
                    (:move next-node))))))
