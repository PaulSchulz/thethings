(ns thethings.core
  (:require [cheshire.core :refer :all]
            [clojurewerkz.machine-head.client :as mh]
            [clojure.pprint]
            [clojure.string :as str]
            ))
;;  (:gen-class))

(defn get-details "" [data]
  [(:app_id data)
   (:dev_id data)
   (:time (:metadata data))]
  )

(defn print-file "" [filename]
  (map parse-string (slurp filename)))

(defn read-file "" [filename]
  (with-open [rdr (clojure.java.io/reader filename)]
    (doseq [line (line-seq rdr)]
      (println line))))

(defn process-file "" [filename]
  (with-open [rdr (clojure.java.io/reader filename)]
    (doseq [line (line-seq rdr)]
      (println (get-details (parse-string line true))))))

(def handler     "thethings.meshed.com.au")
(def application "enfieldlibrary_iot_trial")
(def accesskey   "ttn-account-v2.eH5J27Jr1njZU0Ie7ojrsum9SZzENsDjCCauh7CyD0U") ;; debug
(def device      "roaming_mawsonlakes")
(def topic       (str application "/devices/" device "/up"))
(def topic-down  (str application "/devices/" device "/down"))

;; Example
;; {:app_id enfieldlibrary_iot_trial,
;;  :dev_id roaming_mawsonlakes,
;;  :hardware_serial 5095000000000001,
;;  :port 1,
;;  :counter 49,
;;  :payload_raw cm9hbWluZw==,
;;  :payload_fields {:receivedString roaming},
;;  :metadata {:time 2019-06-17T12:14:35.293368074Z,
;;             :frequency 917,
;;             :modulation LORA,
;;             :data_rate SF7BW125,
;;             :airtime 56576000,
;;             :coding_rate 4/5,
;;             :gateways [{:rssi -54,
;;                         :time 2019-06-17T12:14:35.1365Z,
;;                         :snr 8.5,
;;                         :channel 1,
;;                         :longitude 138.61148,
;;                         :gtw_id eui-b827ebfffe3d50c0,
;;                         :location_source registry,
;;                         :latitude -34.81217,
;;                         :timestamp 1534470899,
;;                         :rf_chain 0,
;;                         :altitude 30}]}}

(defn format-message-heading []
  (let [format-header "%6s  %-28s  %5s  %-4s  %-9s  %7s  %4s  %5s  %4s  %4s"]
    (println
     (str
      (format format-header
              "Count" "Time" "Freq"  "Mode" "Rate"      "Airtime" "Rate" "RSSI"  "SNR"  "GWs")
      "\n"
      (format format-header
              ""      ""     "(MHz)" ""     ""          "(ms)"    ""     "(dBm)" "(dB)" "(#)")
      "\n"
      (format format-header
              "------" "----" "-----" "----" "---------" "-------" "----" "-----" "----" "----")
    ))))

(defn format-message [message]
  (let [format-data "%6d  %-28s  %3.1f  %-4s  %-9s  %7.2f  %4s  %5.1f  %4d  %4d"
        metadata    (:metadata message)
        gateways    (:gateways metadata)]
     (format format-data
             (:counter     message)
             (:time        (nth gateways 0))
             (:frequency   metadata)
             (:modulation  metadata)
             (:data_rate   metadata)
             (/ (:airtime     metadata) 1000000.0)
             (:coding_rate metadata)
             (:snr         (nth gateways 0))
             (:rssi        (nth gateways 0))
             (count gateways)
             )))

(defn ttn-connect []
  (let [conn (mh/connect
              "tcp://thethings.meshed.com.au:1883"
              {:opts {:username           application
                      :password           accesskey
                      :auto-reconnect     true
                      :connection-timeout 3600}
              })]
    (format-message-heading)
    (mh/subscribe conn {topic 0}
                  (fn [^String topic _ ^bytes payload]
                    (println (format-message
                              (parse-string (String. payload "UTF-8") true)))
                    )
                  )
        ))

(defn -main [& args]
  (println "# The Things Network")
  (println "# Device:" device)
  (ttn-connect)
  ;; Infinite stayalive loop
  (loop []
     (Thread/sleep 5000)
     (recur))
  )
