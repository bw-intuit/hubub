(ns hubub.core
  (:require [tentacles.core :as tentacles-core]
            [tentacles.orgs :as orgs]
            [clojure.tools.logging :as log]))

(def ^:dynamic *list-repos-fn* (atom orgs/repos))
(def ^:dynamic *list-teams-fn* (atom orgs/teams))
(def ^:dynamic *create-team-fn* (atom orgs/create-team))

(def ^:dynamic *auth* (atom {:oauth-token (System/getenv "HUBUB_OAUTH_TOKEN")}))

(defn log-list [x] (str "'" (clojure.string/join ", " x) "'"))
(defmulti log-var class)
(defmethod log-var clojure.lang.PersistentVector [x] (log-list x))
(defmethod log-var clojure.lang.PersistentList [x] (log-list x))
(defmethod log-var clojure.lang.LazySeq [x] (log-list x))
(defmethod log-var Number [x] (str "'" x "'"))
(defmethod log-var String [x] (str "'" x "'"))

(defn team-exists?
  [org team-name]
  (let [teams (map :name (@*list-teams-fn* org (assoc @*auth* :all-pages true)))]
    (false? (empty? (some #{team-name} teams)))))

(defn lookup-team-id
  [org team-name]
  (let [teams (@*list-teams-fn* org (assoc @*auth* :all-pages true))]
    (:id (first (filter #(= (:name %) team-name) teams)))))

(defn list-repos [org] (map :name (@*list-repos-fn* org (assoc @*auth* :all-pages true))))

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

(defn- valid-user? [username user-data valid-user-fn] (valid-user-fn username user-data))

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

(defn create-team
  [org repo-name]
  (let [team-name (str repo-name "-contributors")
        options (assoc @*auth* :permission "push")]
    (if (team-exists? org team-name)
      (log/info "Team" (log-var team-name) "already exists.")
      (do
        (log/info "Team" (log-var team-name) "does not exist. creating.")
        (@*create-team-fn* org team-name options)))))

(defn associate-repo-with-team
  [org repo-name]
  (let [team-name (str repo-name "-contributors")
        team-id (lookup-team-id org team-name)]
    (if (orgs/team-repo? team-id org repo-name @*auth*)
      (log/info "Team" (log-var team-name) "already associated with repo" (log-var repo-name))
      (do
        (log/info "team" (log-var team-name) "not associated with repo" (log-var repo-name) ". Associating...")
        (orgs/add-team-repo team-id org repo-name @*auth*)))))

(defn create-teams
  [org]
  (let [repos (list-repos org)]
    (log/info "Organization" (log-var org) "has repos" (log-var repos))
    (doseq [repo-name repos]
      (log/info "Starting to process repo" (log-var repo-name))
      (create-team org repo-name)
      (associate-repo-with-team org repo-name)
      (log/info "Completed processing repo" (log-var repo-name)))))

(defn users-to-remove
  [current-users users]
  (remove #(some #{%} users) current-users))

(defn users-to-add
  [current-users users]
  (remove #(some #{%} current-users) users))

(defn current-users-in-team [id] (map :login (orgs/team-members id @*auth*)))

(defn user-member-state
  [team-id username]
  (let [result (tentacles-core/api-call :get "teams/%s/memberships/%s" [team-id username] @*auth*)]
        (:state result)))

(defn user-member-of-team-pending?
  [team-id username]
  (let [state (user-member-state team-id username)]
    (= state "pending")))

(defn user-member-of-team-active?
  [team-id username]
  (let [state (user-member-state team-id username)]
    (= state "active")))

(defn user-member-of-team?
  [team-id username]
  (if (or (user-member-of-team-active? team-id username)
          (user-member-of-team-pending? team-id username))
    true
    false))

(defn add-team-member-fork
  "Fork of tentacles/add-team-member to check for state of added member"
  [id user options]
  (let [result (tentacles-core/api-call :put "teams/%s/memberships/%s" [id user] options)]
    (log/debug "Result adding user" (log-var user) "to" (log-var id) ":" result)
    (log/info "User state is" (log-var (:state result)))
    (let [state (:state result)]
      (if (= (false? (nil? (some #{state} ["active" "pending"])))) true false))))

;(def ironman-vnd "application/vnd.github.ironman-preview+json")

(defn add-user-to-team
  [id user]
  (if (user-member-of-team? id user)
    true
    (orgs/add-team-member id user @*auth*)))

(defn remove-user-from-team
  [id user]
  (orgs/delete-team-member id user @*auth*))

(defn remove-users-from-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Removing user" (log-var user) "from" (log-var team-name))
    (if (false? (remove-user-from-team team-id user))
      (let [msg (str "Error removing " (log-var user) " from " (log-var team-id))]
        (throw (Exception. msg))))))

(defn add-users-to-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Adding user" (log-var user) "to" (log-var team-name))
    (if (false? (add-user-to-team team-id user))
      (let [msg (str "Error adding " (log-var user) " from " (log-var team-id))]
        (throw (Exception. msg))))))

(defn set-team-users
  [org team-name users]
  (let [team-id (lookup-team-id org team-name)
        current-users (current-users-in-team team-id)
        users-to-remove (users-to-remove current-users users)
        users-to-add (remove #(user-member-of-team-pending? team-id %)
                             (users-to-add current-users users))]
    (log/info "Current list of users in" (log-var team-name) (log-var current-users))
    (log/info "Users to remove from" (log-var team-name) (log-var users-to-remove))
    (log/info "Users to add from" (log-var team-name) (log-var users-to-add))
    (remove-users-from-repo users-to-remove team-name team-id)
    (add-users-to-repo users-to-add team-name team-id)))

(defn set-users
  [org input valid-user-fn]
  (doseq [repo-name (list-repos org)]
    (let [users (repo-users repo-name input valid-user-fn)]
      (log/info "Setting users for" (log-var repo-name) "to" (log-var users))
      (set-team-users org (str repo-name "-contributors") users))))

(defn- check-env
  []
  (if (nil? (:oauth-token @*auth*))
    (throw (Exception. "HUBUB_OAUTH_TOKEN not set"))))

(defn- process
  [org input valid-user-fn]
    (do
      (check-env)
      (create-teams org)
      (set-users org input valid-user-fn)))

(defn run
  ([org input]
    (do
      (log/info "Not performing custom user verification")
      (process org input (fn [x y] true))))
  ([org input valid-user-fn]
    (do
      (log/info "Processing with user provided verify function")
      (process org input valid-user-fn))))
