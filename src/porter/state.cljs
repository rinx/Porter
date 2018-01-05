(ns porter.state
  (:require [om.next :as om]
            [re-natal.support :as sup]
            [cljs.core.async :as async
                             :refer [take!]]
            [porter.http :as http]))

(defonce app-state
  (let [atm (atom {:app/urls []
                   :app/scripts []})]
    (take! (http/fetch-all-script)
      (fn [scripts]
        (->> scripts
          (sort-by :id)
          (reverse)
          (swap! atm assoc :app/scripts))))
    atm))

(defmulti read om/dispatch)

(defmethod read :app/urls
  [env key params]
  (let [state (:state env)]
    {:value (:app/urls @state)}))

(defmethod read :app/scripts
  [env key params]
  (let [state (:state env)]
    {:value (:app/scripts @state)}))

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    (if-let [[_ v] (find st k)]
      {:value v}
      {:value :not-found})))

(defn- gen-id [urls]
  (->> (map :id urls)
       (apply max)
       (inc)))

(defmulti mutate om/dispatch)

(defmethod mutate 'urls/add
  [env key params]
  (let [state (:state env)
        urls (:urls @state)
        id (gen-id urls)
        new-url (assoc params :id id)]
    {:value  {:keys [:app/urls]}
     :action
     (fn []
       (swap! state update :app/urls conj new-url))}))

(defmethod mutate 'scripts/add
  [env key params]
  (let [state (:state env)
        scripts (:app/scripts @state)
        id (gen-id scripts)
        new-script (assoc params :id id)]
    {:value  {:keys [:app/scripts]}
     :action
     (fn []
       (swap! state update :app/scripts conj new-script))}))

(defmethod mutate 'scripts/update
  [env key params]
  (let [state (:state env)
        scripts (:scripts params)]
    {:value  {:keys [:app/scripts]}
     :action
     (fn []
       (swap! state assoc :app/scripts scripts))}))

(defonce reconciler
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read
                         :mutate mutate})
     :root-render sup/root-render
     :root-unmount sup/root-unmount}))
