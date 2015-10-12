(ns hubub.core
  (:require [tentacles.core :as tentacles-core]
            [tentacles.orgs :as orgs]
            [tentacles.users :as users]
            [clojure.tools.logging :as log]))

(def ^:dynamic *list-repos-fn* (atom orgs/repos))
(def ^:dynamic *list-teams-fn* (atom orgs/teams))
(def ^:dynamic *create-team-fn* (atom orgs/create-team))

(def ^:dynamic *auth* (atom {:oauth-token (System/getenv "HUBUB_OAUTH_TOKEN")}))

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

(defn- valid-user?
  [username user-data valid-user-fn]
  (if (nil? username)
    false
    (valid-user-fn username user-data)))

(defn repo-users
  [repo-name input valid-user-fn]
  (let [repos-user-map (parse-repos-to-users input)
        repo-user-map (get repos-user-map repo-name)
        valid-repo-users (filter (fn [username]
                                   (let [user-data (get input username)]
                                     (valid-user? username user-data valid-user-fn)))
                                 repo-user-map)]
    (do
      (log/info "valid repo users for" repo-name ":" valid-repo-users)
      valid-repo-users)))

(defn create-team
  [org repo-name]
  (let [team-name (str repo-name "-contributors")
        options (merge @*auth* {:repo-names [repo-name]
                                :permission "push"})]
    (if (team-exists? org team-name)
      (do
        (log/info "team" team-name "already exists.")
        team-name)
      (do
        (log/info "team" team-name "does not exist. creating.")
        (:name (@*create-team-fn* org team-name options))))))

(defn create-team
  [org repo-name]
  (let [team-name (str repo-name "-contributors")
        options (merge @*auth* {:repo-names [repo-name]
                                :permission "push"})]
    (if (team-exists? org team-name)
      (do
        (log/info "team" team-name "already exists.")
        team-name)
      (do
        (log/info "team" team-name "does not exist. creating.")
        (@*create-team-fn* org team-name options)

        (log/info "adding" repo-name "to" team-name)
        (let [team-id (lookup-team-id org team-name)]
          (orgs/add-team-repo team-id org repo-name @*auth*))

        team-name))))

(defn create-teams
  [org]
  (let [repos (list-repos org)]
    (log/info org "has repos" repos)
    (doseq [repo-name repos]
      (log/info "processing" repo-name)
      (create-team org repo-name))))

(defn users-to-remove
  [current-users users]
  (remove #(some #{%} users) current-users))

(defn users-to-add
  [current-users users]
  (remove #(some #{%} current-users) users))

(defn current-users-in-team
  [id]
  (map :login (orgs/team-members id @*auth*)))

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
    (log/debug "Result adding user" user "to" id ":" result)
    (log/info "User state is" (:state result))
    (let [state (:state result)]
      (if (= (false? (nil? (some #{state} ["active" "pending"])))) true false))))

(def ironman-vnd "application/vnd.github.ironman-preview+json")

(defn add-user-to-team
  [id user]
  (if (user-member-of-team? id user)
    true
    (add-team-member-fork id user (assoc @*auth* :accept ironman-vnd))))

(defn remove-user-from-team
  [id user]
  (orgs/delete-team-member id user @*auth*))

(defn remove-users-from-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Removing user" user "from" team-name)
    (if (true? (remove-user-from-team team-id user))
      true
      (throw (Exception. (str "Error removing " user " from " team-id))))))

(defn add-users-to-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Adding user" user "to" team-name)
    (if (true? (add-user-to-team team-id user))
      true
      (throw (Exception. (str "Error adding " user " from " team-id))))))

(defn set-team-users
  [org team-name users]
  (let [team-id (lookup-team-id org team-name)
        current-users (current-users-in-team team-id)
        users-to-remove (users-to-remove current-users users)
        users-to-add (remove #(user-member-of-team-pending? team-id %)
                             (users-to-add current-users users))]
    (log/info "Current list of users in" team-name current-users)
    (log/info "Users to remove:" users-to-remove)
    (log/info "Users to add" users-to-add)
    (remove-users-from-repo users-to-remove team-name team-id)
    (add-users-to-repo users-to-add team-name team-id)))

(defn set-users
  [org input valid-user-fn]
  (doseq [repo-name (list-repos org)]
    (let [users (repo-users repo-name input valid-user-fn)]
      (log/info "Setting users for" repo-name "to" users)
      (set-team-users org (str repo-name "-contributors") users))))

(defn check-env
  []
  (if (nil? (:oauth-token @*auth*))
    (throw (Exception. "HUBUB_OAUTH_TOKEN not set"))))

(defn run
  ([org input]
    (do
      (log/info "Not performing custom user verification")
      (check-env)
      (create-teams org)
      (set-users org input (fn [x y] true))))
  ([org input valid-user-fn]
    (do
      (log/info "Processing with user provided verify function")
      (check-env)
      (create-teams org)
      (set-users org input valid-user-fn))))
