(ns hubub.core
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging :as log]
            [hubub.github :as github]
            [hubub.parsers :as p]))

(defn- create-teams
  [org]
  (let [repos (github/list-repos org)]
    (log/info "Organization" (p/log-var org) "has repos" (p/log-var repos))
    (doseq [repo-name repos]
      (log/info "Starting to process repo" (p/log-var repo-name))
      (github/create-team org repo-name)
      (github/associate-repo-with-team org repo-name)
      (log/info "Completed processing repo" (p/log-var repo-name)))))

(defn- remove-users-from-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Removing user" (p/log-var user) "from" (p/log-var team-name))
    (if (false? (github/remove-user-from-team team-id user))
      (let [msg (str "Error removing " (p/log-var user) " from " (p/log-var team-id))]
        (throw (Exception. msg))))))

(defn- add-users-to-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Adding user" (p/log-var user) "to" (p/log-var team-name))
    (if (false? (github/add-user-to-team team-id user))
      (let [msg (str "Error adding " (p/log-var user) " from " (p/log-var team-id))]
        (throw (Exception. msg))))))

(defn- set-team-users
  [org team-name users]
  (let [team-id (github/lookup-team-id org team-name)
        current-users (github/current-users-in-team team-id)
        users-to-remove (p/users-to-remove current-users users)
        users-to-add (remove #(github/user-member-of-team-pending? team-id %)
                             (p/users-to-add current-users users))]
    (do
      (log/info "Current list of users in" (p/log-var team-name) (p/log-var current-users))
      (log/info "Users to remove from" (p/log-var team-name) (p/log-var users-to-remove))
      (log/info "Users to add from" (p/log-var team-name) (p/log-var users-to-add))
      (remove-users-from-repo users-to-remove team-name team-id)
      (add-users-to-repo users-to-add team-name team-id))))

(defn- set-users
  [org input valid-user-fn]
  (doseq [repo-name (github/list-repos org)]
    (let [users (p/repo-users repo-name input valid-user-fn)]
      (log/info "Setting users for" (p/log-var repo-name) "to" (p/log-var users))
      (set-team-users org (str repo-name "-contributors") users))))

(defn- process
  [org input token valid-user-fn]
    (do
      (github/set-github-token token)
      (create-teams org)
      (set-users org input valid-user-fn)))

(defn run
  ([org input token]
    (do
      (log/info "Not performing custom user verification")
      (process org input token (fn [x y] true))))
  ([org input token valid-user-fn]
    (do
      (log/info "Processing with user provided verify function")
      (process org input token valid-user-fn))))
