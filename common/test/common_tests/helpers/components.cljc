;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns common-tests.helpers.components
  (:require
   [app.common.data.macros :as dm]
   [app.common.files.changes-builder :as pcb]
   [app.common.files.helpers :as cfh]
   [app.common.geom.point :as gpt]
   [app.common.logic.libraries :as cll]
   [app.common.types.component :as ctk]
   [app.common.types.components-list :as ctkl]
   [app.common.types.container :as ctn]
   [app.common.types.file :as ctf]
   [app.common.types.pages-list :as ctpl]
   [app.common.types.shape-tree :as ctst]
   [common-tests.helpers.files :as thf]
   [common-tests.helpers.ids-map :as thi]
   [common-tests.helpers.shapes :as ths]))

(defn make-component
  [file label root-label & {:keys [] :as params}]
  (let [page (thf/current-page file)
        root (ths/get-shape file root-label)]

    (dm/assert!
     "Need that root is already a frame"
     (cfh/frame-shape? root))

    (let [[_new-root _new-shapes updated-shapes]
          (ctn/convert-shape-in-component root (:objects page) (:id file))

          updated-root (first updated-shapes)] ; Can't use new-root because it has a new id

      (thi/set-id! label (:component-id updated-root))

      (ctf/update-file-data
       file
       (fn [file-data]
         (as-> file-data $
           (reduce (fn [file-data shape]
                     (ctpl/update-page file-data
                                       (:id page)
                                       #(update % :objects assoc (:id shape) shape)))
                   $
                   updated-shapes)
           (ctkl/add-component $ (assoc params
                                        :id (:component-id updated-root)
                                        :name (:name updated-root)
                                        :main-instance-id (:id updated-root)
                                        :main-instance-page (:id page)
                                        :shapes updated-shapes))))))))

(defn get-component
  [file label]
  (ctkl/get-component (:data file) (thi/id label)))

(defn get-component-by-id
  [file id]
  (ctkl/get-component (:data file) id))

(defn set-child-label
  [file shape-label child-idx label]
  (let [id (-> (ths/get-shape file shape-label)
               :shapes
               (nth child-idx))]
    (when id
      (thi/set-id! label id))))

(defn instantiate-component
  [file component-label copy-root-label & {:keys [parent-label library children-labels] :as params}]
  (let [page      (thf/current-page file)
        library   (or library file)
        component (get-component library component-label)
        parent-id (when parent-label
                    (thi/id parent-label))
        parent    (when parent-id
                    (ctst/get-shape page parent-id))
        frame-id  (if (cfh/frame-shape? parent)
                    (:id parent)
                    (:frame-id parent))

        [copy-root copy-shapes]
        (ctn/make-component-instance page
                                     component
                                     (:data library)
                                     (gpt/point 100 100)
                                     true
                                     {:force-id (thi/new-id! copy-root-label)
                                      :force-frame-id frame-id})

        copy-root' (cond-> copy-root
                     (some? parent)
                     (assoc :parent-id parent-id)

                     (some? frame-id)
                     (assoc :frame-id frame-id)

                     (and (some? parent) (ctn/in-any-component? (:objects page) parent))
                     (dissoc :component-root))
        file'      (ctf/update-file-data
                    file
                    (fn [file-data]
                      (as-> file-data $
                        (ctpl/update-page $
                                          (:id page)
                                          #(ctst/add-shape (:id copy-root')
                                                           copy-root'
                                                           %
                                                           frame-id
                                                           parent-id
                                                           nil
                                                           true))
                        (reduce (fn [file-data shape]
                                  (ctpl/update-page file-data
                                                    (:id page)
                                                    #(ctst/add-shape (:id shape)
                                                                     shape
                                                                     %
                                                                     (:parent-id shape)
                                                                     (:frame-id shape)
                                                                     nil
                                                                     true)))
                                $
                                (remove #(= (:id %) (:did copy-root')) copy-shapes)))))]
    (when children-labels
      (dotimes [idx (count children-labels)]
        (set-child-label file' copy-root-label idx (nth children-labels idx))))
    file'))

(defn component-swap
  [file shape-label new-component-label new-shape-label & {:keys [library] :as params}]
  (let [shape            (ths/get-shape file shape-label)
        library          (or library file)
        libraries        {(:id library) library}
        page             (thf/current-page file)
        objects          (:objects page)
        id-new-component (-> (get-component library new-component-label)
                             :id)

        ;; Store the properties that need to be maintained when the component is swapped
        keep-props-values (select-keys shape ctk/swap-keep-attrs)


        [new_shape _ changes]
        (-> (pcb/empty-changes nil (:id page))
            (cll/generate-component-swap objects shape (:data file) page libraries id-new-component 0 nil keep-props-values))]

    (thi/set-id! new-shape-label (:id new_shape))
    (thf/apply-changes file changes)))
