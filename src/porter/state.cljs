(ns porter.state
  (:require [om.next :as om]
            [re-natal.support :as sup]))

(defonce app-state (atom {:urls []
                          :scripts []}))

(defmulti read om/dispatch)

(defmethod read :urls
  [env key params]
  (let [state (:state env)]
    {:value (:urls @state)}))

(defmethod read :scripts
  [env key params]
  (let [state (:state env)]
    {:value (:scripts @state)}))

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
    {:action
     (fn []
       (swap! state update :urls conj new-url))}))

(defmethod mutate 'scripts/add
  [env key params]
  (let [state (:state env)
        scripts (:scripts @state)
        id (gen-id scripts)
        new-script (assoc params :id id)]
    {:action
     (fn []
       (swap! state update :scripts conj new-script))}))

(defmethod mutate 'scripts/update
  [env key params]
  (let [state (:state env)
        scripts (:scripts params)]
    {:action
     (fn []
       (swap! state assoc :scripts scripts))}))

(defonce reconciler
         (om/reconciler
           {:state        app-state
            :parser       (om/parser {:read read
                                      :mutate mutate})
            :root-render  sup/root-render
            :root-unmount sup/root-unmount}))
