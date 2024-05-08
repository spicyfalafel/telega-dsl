(ns tg-dialog.misc)

(defn dissoc-path [m path]
  (assert (vector? path))
  (if (= 1 (count path))
    (dissoc m (first path))
    (update-in m (pop path) dissoc (peek path))))

(defn index-of [x coll]
  (let [idx? (fn [i a] (when (= x a) i))]
  (first (keep-indexed idx? coll))))
