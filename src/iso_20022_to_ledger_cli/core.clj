(ns iso-20022-to-ledger-cli.core
  (:gen-class)
  (:require [clojure.data.zip.xml :refer [attr text xml-> xml1->]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [java-time :as time]
            [clojure.zip :as zip]))

(def param-mapping
  "Mapping from ISO 20022 to parameters. See [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022)"
  {:amount [:Amt]
   :currency [:Amt (attr :Ccy)]
   :booking-date [:BookgDt :Dt]
   :value-date [:ValDt :Dt]
   :info [:AddtlNtryInf]
   :reference [:AcctSvcrRef]
   :type [:CdtDbtInd]
   })

(defn get-attr
  "Get the attr for the given `path` in the given `entry`.
  Returns nil if there is no such attr"
  [entry path]
  (some->
   (apply xml1-> entry path)))

(defn get-field
  "Get the field text for the given `path` in the given `entry`.
  Returns nil if there is no such field"
  [entry path]
  (some->
   (apply xml1-> entry path) text string/trim))

(defn get-type [type]
  (case type
    "DBIT" :debit
    "CRDT" :credit))

(defn get-date [s]
  (time/local-date s))

(defn read-file
  "Read an export file from VUBIS and return a map with all the data"
  [file]
  (let [root (-> file io/file xml/parse zip/xml-zip)]
    (for [entry (xml-> root :Document :BkToCstmrStmt :Stmt :Ntry)]
      (->> (for [[key path] param-mapping
                 :let [val (cond
                             (#{:currency} key) (get-attr entry path)
                             (#{:type} key) (get-type (get-field entry path))
                             (#{:booking-date :value-date} key) (get-date (get-field entry path))
                             :else (get-field entry path))]
                 :when (some? val)]
             [key val])
           (into {})))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
