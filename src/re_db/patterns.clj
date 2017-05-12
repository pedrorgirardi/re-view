(ns re-db.patterns)

(defmacro capture-patterns
  "Evaluates body, returning map with evaluation result and read patterns."
  [& body]
  `(binding [~'re-db.patterns/*pattern-log* ~'re-db.patterns/empty-pattern-map]
     (let [value# (do ~@body)]
       {:value    value#
        :patterns (when (not= ~'re-db.patterns/*pattern-log* ~'re-db.patterns/empty-pattern-map)
                    ~'re-db.patterns/*pattern-log*)})))