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
