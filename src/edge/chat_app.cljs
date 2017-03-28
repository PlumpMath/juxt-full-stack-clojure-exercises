;; Copyright © 2016, 2017, JUXT LTD.

;; Here's a simple single page app written in ClojureScript that uses
;; Reagent.

(ns edge.chat-app
  (:require
   [reagent.core :as r]
   [cljs.reader :refer [read-string]]))

(def app-state (r/atom {:messages []}))

(defn get-chat-data []
  (println "Loading chat data")
  (doto
      (new js/XMLHttpRequest)
      (.open "GET" "/chat/messages")
      (.setRequestHeader "Accept" "application/edn")
      (.addEventListener
       "load"
       (fn [evt]
         (swap! app-state
                assoc :chat
                (read-string evt.currentTarget.responseText))))
      (.send)))

(defn needs-saving? [state]
  (when-let [db-entry (get (:phonebook state)
                           (first (:current state)))]
    (not= (second (:current state)) db-entry)))

(defn changer [path]
  (fn [ev]
    (swap! app-state assoc-in path (.-value (.-target ev)))))

(defn chat []
  (fn []
    (let [state @app-state]
      [:div
       [:p "Chat Messages"]
       [:table
        [:tbody
         (map-indexed (fn [idx message]
                        [:tr {:key idx} [:td message]])
                      (:messages state))]]

       (when-let [[id entry] (:current state)]
         [:div.container
          [:h3 (:firstname entry) " " (:surname entry)]
          [:form
           {:on-submit (fn [ev] (.preventDefault ev))}
           [:p
            [:label "Id"]
            [:input {:type "text"
                     :disabled true
                     :value id}]]
           (for [[k label] [[:firstname "Firstname"]
                            [:surname "Surname"]
                            [:phone "Phone"]]]
             ^{:key (keyword "field" k)}
             [:p
              [:label label]
              [:input {:type "text"
                       :value (get entry k)
                       :on-change (changer [:current 1 k])}]])

           [:button {:disabled (not (needs-saving? state))
                     :on-click (fn [ev] (save-entry state))} "Save"]

           [:button {:on-click (fn [ev] (delete-entry state))} "Delete"]]])])))

(defn init [section]
  ;; (get-chat-data)

  (println "Subscribing")
  (let [es (new js/EventSource "/chat/messages")]
    (.addEventListener
     es "message"
     (fn [ev]
       (swap! app-state update-in [:messages] conj (.-data ev)))))

  (r/render-component [chat] section))
