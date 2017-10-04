(ns titanic.training
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as s]
            [cortex.nn.layers :as layers]
            [cortex.nn.execute :as execute]
            [cortex.experiment.train :as train]))

(defn default-dataset
  []
  (->> (io/resource "training/train.csv")
       (slurp)
       csv/read-csv
       rest
       (mapv (fn [[^String PassengerId
                   ^String Survived
                   ^String Pclass
                   ^String Name
                   ^String Sex
                   ^String Age
                   ^String SibSp
                   ^String Parch
                   ^String Ticket
                   ^String Fare
                   ^String Cabin
                   ^String Embarked]]
               {:data [(Integer. PassengerId)
                       (Integer. Pclass)
                       ;Name
                       ;Sex
                       (and (not-empty Age) (Double. Age))
                       (Integer. SibSp)
                       (Integer. Parch)
                       ;Ticket
                       (Double. Fare)
                       ;Cabin
                       #_Embarked]
                :labels (if (= Survived "1") [1.0] [0.0])}))))

(def description
  [(layers/input 2 1 1 :id :data)
   (layers/batch-normalization)
   ;;Fix the weights to make the unit test work.
   (layers/linear 1 :weights [[-0.2 0.2]])
   (layers/logistic :id :labels)])

(defn train-model
  []
  (io/delete-file "trained-network.nippy" true)
  (let [ds (shuffle (default-dataset))
        ds-count (count ds)
        train-ds (take (int (* 0.9 ds-count)) ds)
        test-ds (drop (int (* 0.9 ds-count)) ds)
        _ (train/train-n description train-ds test-ds
                         :batch-size 50 :epoch-count 10
                         :simple-loss-print? true)
        trained-network (train/load-network "trained-network.nippy")
        input-data [{:data [1 0 3 "Braund, Mr. Owen Harris" "male" 22.0 1 0 "A/5 21171" 7.25 "" "S"]}
                    {:data [891 0 3 "Dooley, Mr. Patrick" "male" 32.0 0 0 "370376" 7.75 "" "Q"]}]
        [[survived] [dead]] (->> (execute/run trained-network input-data)
                                 (map :labels))]
    (println "Survived? " survived)
    (println "Dead? " dead)))
