(ns hubub.core-test
  (:require [clojure.test :refer :all]
            [hubub.stubs :refer :all]
            [hubub.core :refer :all]))

(defn stub-all
  []
  (reset! *auth* {:oauth-token "abc"})
  (reset! *list-repos-fn* list-repos-stub-fn)
  (reset! *list-teams-fn* list-teams-stub-fn)
  (reset! *create-team-fn* create-team-stub-fn))

(deftest verify-list-repos
  (testing "verify listing repos"
    (do
      (stub-all)
      (is (= (list-repos "org") ["project1"])))))

(deftest team-exists-test
  (do
    (stub-all)
    (testing "valid team returns true"
      (is (= (team-exists? "org" "Owners") true)))
    (testing "valid team returns false"
      (is (= (team-exists? "org" "invalid") false)))))

(deftest verify-repo-users
  (testing "make sure filters proper users"
    (is (= (repo-users "repo1" input valid-user-stub-fn)
           ["github-user1" "github-user3"]))
    (is (= (repo-users "repo2" input valid-user-stub-fn)
           ["github-user3"]))))

(deftest create-team-test
  (do 
    (stub-all)
    (testing "create team"
      (is (= (:name (create-team "org" "team1")) "team1-contributors")))))

(deftest parse-repos-to-user-test
  (testing "test parse-repos-to-user"
    (is (= (parse-repos-to-users input)
           {"repo1" ["github-user1" "github-user2" "github-user3"]
            "repo2" ["github-user2" "github-user3"]}))))
