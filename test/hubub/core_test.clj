(ns hubub.core-test
  (:require [clojure.test :refer :all]
            [hubub.stubs :refer :all]
            [hubub.core :refer :all]))

(deftest team-exists-test
  (testing "error access teams"
    (is (thrown-with-msg? Exception
                          #"error calling github"
                          (team-exists? "error" (list-teams-stub-error "org")))))
  (testing "valid team returns true"
    (is (true? (team-exists? "Owners" (list-teams-stub-success "org")))))
  (testing "invalid team returns false"
    (is (false? (team-exists? "bad" (list-teams-stub-success "org"))))))

(deftest valid-repo-users-test
  (testing "return valid user for given repo"
    (is (= (valid-repo-users "repo1" input valid-user-stub-fn ["admin1"])
           ["admin1" "github-user1" "github-user3"]))))

(deftest invalid-users-test
  (testing "returns invalid users"
    (is (= (invalid-users ["user1" "user2" "user3"] ["user1" "user3" "user4"])
           ["user2"]))
    (is (= (invalid-users [] ["user1"])
           []))
    (is (= (invalid-users ["user1" "user2" "user3"] ["user1" "user3"])
           ["user2"]))))
