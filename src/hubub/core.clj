(ns hubub.core
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging :as log]
            [hubub.github :as github]
            [hubub.parsers :as p]))

(def ^:dynamic *errors* (atom '()))

(defn- process-error
  [message]
  (do
    (log/error message)
    (swap! *errors* conj message)))

(def valid-permissions ["push" "admin"])

(defn- gen-team-name [repo-name access] (str repo-name "-" access))

(defn- gh-error? [result] (false? (nil? (:status result))))

(defn- throw-github-error [] (throw (ex-info "error calling github" {})))

(defn- create-team-options [permission] (assoc @github/*auth* :permission permission))

(defn team-exists?
  [team-name gh-list-teams-result]
  (if (gh-error? gh-list-teams-result)
    (throw-github-error)
    (let [teams (map :name gh-list-teams-result)]
      (false? (empty? (some #{team-name} teams))))))

(defn create-team-unless-exists
  [org repo-name permission]
  (let [team-name (gen-team-name repo-name permission)
        team-exists (team-exists? team-name (github/gh-list-teams org))]
    (log/info "Creating team" team-name)
    (if team-exists
      true
      (do
        (github/gh-create-team org team-name (create-team-options permission))
        (github/associate-repo-with-team org repo-name team-name)))))

(defn create-repo-teams
  [org repo-name]
  (doseq [perm valid-permissions]
    (create-team-unless-exists org repo-name perm)))

(defn- create-teams
  [org]
  (let [repos (github/list-repos org)]
    (log/info "Organization" (p/log-var org) "has repos" (p/log-var repos))
      (doseq [repo-name repos]
        (log/info "Starting to create teams for repo" (p/log-var repo-name))
        (create-repo-teams org repo-name)
        (log/info "Completed creating teams for repo" (p/log-var repo-name)))))

(defn- remove-users-from-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Removing user" (p/log-var user) "from" (p/log-var team-name))
    (if (false? (github/remove-user-from-team team-id user))
      (let [msg (str "Error removing " (p/log-var user) " from " (p/log-var team-id))]
        (process-error msg)))))

(defn- add-users-to-repo
  [users team-name team-id]
  (loop [u users]
    (if (empty? u)
      nil
      (do
        (let [user (first u)]
          (log/info "Adding user" (p/log-var user) "to" (p/log-var team-name))
          (if (github/add-user-to-team team-id user)
            (log/info "Successfully added " (p/log-var user) "to" (p/log-var team-name))
            (process-error (str "Unable to add " (p/log-var user)
                                " to " (p/log-var team-name)
                                " make sure they have accepted membership to the org")))
          (recur (rest u)))))))

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
  [org input repo-name team-name valid-user-fn access]
  (let [users (p/repo-users repo-name input valid-user-fn access)]
    (log/info "Setting users for" (p/log-var repo-name) "to" (p/log-var users))
    (set-team-users org team-name users)))

(defn- set-organizaiton-users
  [org input valid-user-fn]
  (doseq [repo-name (github/list-repos org)]
    (doseq [access valid-permissions]
      (let [team-name (str repo-name "-" access)]
        (set-users org input repo-name team-name valid-user-fn access)))))

(defn- process
  [org input token valid-user-fn]
    (do
      (github/set-github-token token)
      (create-teams org)
      (set-organizaiton-users org input valid-user-fn)

      (if (empty? @*errors*)
        nil
        (do
          (log/error "Received" (count @*errors*) "errors")
          (doseq [error @*errors*]
            (log/error "Received error: " error))))))

(defn run
  ([org input token]
    (do
      (log/info "Not performing custom user verification")
      (process org input token (fn [x y] true))))
  ([org input token valid-user-fn]
    (do
      (log/info "Processing with user provided verify function")
      (process org input token valid-user-fn))))
