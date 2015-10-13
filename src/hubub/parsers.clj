(ns hubub.parsers
  (:require [clojure.tools.logging :as log]))

(defn- log-list [x] (str "'" (clojure.string/join ", " x) "'"))
(defmulti log-var class)
(defmethod log-var clojure.lang.PersistentVector [x] (log-list x))
(defmethod log-var clojure.lang.PersistentList [x] (log-list x))
(defmethod log-var clojure.lang.LazySeq [x] (log-list x))
(defmethod log-var Number [x] (str "'" x "'"))
(defmethod log-var String [x] (str "'" x "'"))

(defn- valid-user? [username user-data valid-user-fn] (valid-user-fn username user-data))

(defn parse-repos-to-users
  [input]
  (loop [users input result {}]
    (if (empty? users)
        result
        (let [user (first users)
              username (first user)
              repos (get (last user) "repos")]
          (recur (rest users) (loop [r repos result2 result]
                                (if (empty? r)
                                  result2
                                  (let [repo (first r)]
                                    (recur (rest r)
                                           (assoc result2 repo (vec (concat (get result2 repo) [username]))))))))))))

(defn repo-users
  [repo-name input valid-user-fn]
  (let [repos-user-map (parse-repos-to-users input)
        repo-user-map (get repos-user-map repo-name)
        valid-repo-users (filter (fn [username]
                                   (let [user-data (get input username)]
                                     (valid-user? username user-data valid-user-fn)))
                                 repo-user-map)]
    (do
      (log/info "Valid repo users for" (log-var repo-name) "are" (log-var valid-repo-users))
      valid-repo-users)))

(defn users-to-remove
  [current-users users]
  (remove #(some #{%} users) current-users))

(defn users-to-add
  [current-users users]
  (remove #(some #{%} current-users) users))
