(ns porter.state
  (:require [om.next :as om]
            [re-natal.support :as sup]))

(defonce app-state (atom {:urls []}))

(defmulti read om/dispatch)
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
        id (gen-id (:urls @state))
        new-url (assoc params :id id)]
    {:action
     (fn []
       (swap! state update :urls conj new-url))}))

(defonce reconciler
         (om/reconciler
           {:state        app-state
            :parser       (om/parser {:read read
                                      :mutate mutate})
            :root-render  sup/root-render
            :root-unmount sup/root-unmount}))
