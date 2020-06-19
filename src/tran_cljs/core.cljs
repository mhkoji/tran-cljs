(ns tran-cljs.core
  (:require [clojure.browser.repl :as repl]
            [cljs.core.async :refer [go <! chan put!]]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

;;

(defrecord LoadingStep [on-loaded])

(defn load-text [loading-step]
  (js/setTimeout #((-> loading-step :on-loaded) "blah") 1000))

(defn loading-component [loading-step]
  (load-text loading-step)
  (fn [loading-step]
    [:span "Loading..."]))

;;

(defrecord EditingStep [swap-self text on-save])

(defn update-text [editing-step text]
  ((-> editing-step :swap-self) #(assoc % :text text)))

(defn call-on-save [editing-step]
  ((-> editing-step :on-save) (-> editing-step :text)))

(defn editing-component [editing-step]
  [:div
   [:input {:value (-> editing-step :text)
            :on-change #(update-text editing-step
                                     (.-value (.-target %)))}]
   [:button
    {:on-click #(call-on-save editing-step)}
    "Save"]])

;;

(defrecord SavingStep [swap-self text saved-p])

(defn update-saved [saving-step]
  ((-> saving-step :swap-self) #(assoc % :saved-p true)))

(defn save-text [saving-step]
  (js/setTimeout #(update-saved saving-step) 1000))

(defn saving-component [saving-step]
  (save-text saving-step)
  (fn [saving-step]
    (if (-> saving-step :saved-p)
      [:div
       [:span "Saved!"]]
      [:div
       [:input {:value (-> saving-step :text)
                :disabled true
                :readOnly true}]
       [:span "Saving..."]])))

;;

(defmulti step-component (fn [step] (type step)))

(defmethod step-component LoadingStep [step]
  [loading-component step])

(defmethod step-component EditingStep [step]
  [editing-component step])

(defmethod step-component SavingStep [step]
  [saving-component step])

;;

(defn page [step]
  [:div
   [:h1 "Edit the text!"]
   [step-component step]])

(defn render-loop [{:keys [create-store render]}]
  (let [store (r/atom nil)]
    (reset! store (create-store #(swap! store %)))
    (fn []
      (render @store))))

(defn main-loop []
  (render-loop
   {:create-store
    (fn [swap-fn]
      (->LoadingStep
       (fn [text]
         (swap-fn
          (fn [_]
            (->EditingStep
             swap-fn text
             (fn [text]
               (swap-fn
                (fn [_]
                  (->SavingStep swap-fn text false))))))))))
    :render
    (fn [store] (page store))}))

(rdom/render [main-loop] (js/goog.dom.getElement "app"))
