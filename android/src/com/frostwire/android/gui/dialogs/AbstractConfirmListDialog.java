/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import com.frostwire.android.R;
import com.frostwire.android.gui.views.AbstractDialog;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.logging.Logger;

import java.util.*;

/**
 * This dialog should evolve to allow us for reuse on a number of situations in which you
 * need a dialog that needs to display a list view control.
 * 
 * This would be the simplest version, in the future it will have a text editor to filter
 * the contents of the list, and it will also support different modes of selection.
 * 
 * For now it just uses an adapter to display the contents of the model data.
 * 
 * It's up to the user to implement the adapter (hmm, perhaps that's where the selection mode logic should be)
 * 
 * @author aldenml
 * @author gubatron
 * @author votaguz
 */
@SuppressWarnings("ALL")
abstract class AbstractConfirmListDialog<T> extends AbstractDialog implements
        AbstractListAdapter.OnItemCheckedListener {

    private static final String BUNDLE_KEY_DIALOG_ICON = "dialogIcon";
    protected static final String BUNDLE_KEY_CHECKED_OFFSETS = "checkedOffsets";
    private static final String BUNDLE_KEY_DIALOG_TITLE = "title";
    private static final String BUNDLE_KEY_DIALOG_TEXT = "dialogText";
    private static final String BUNDLE_KEY_LIST_DATA = "listData";
    private static final String BUNDLE_KEY_SELECTION_MODE = "selectionMode";
    private static final String BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX = "lastSelectedRadioButtonIndex";

    /**
     * TODOS: 1. Add an optional text filter control that will be connected to the adapter.
     */

    private Logger LOGGER = Logger.getLogger(AbstractConfirmListDialog.class);
    private CompoundButton.OnCheckedChangeListener selectAllCheckboxOnCheckedChangeListener;

    enum SelectionMode {
        NO_SELECTION,
        SINGLE_SELECTION,
        MULTIPLE_SELECTION;

       public static SelectionMode fromInt(int n) {
           SelectionMode selectionMode = SelectionMode.NO_SELECTION;
           if (n == SelectionMode.MULTIPLE_SELECTION.ordinal()) {
               selectionMode = SelectionMode.MULTIPLE_SELECTION;
           } else if (n == SelectionMode.SINGLE_SELECTION.ordinal()) {
               selectionMode = SelectionMode.SINGLE_SELECTION;
           }
           return selectionMode;
       }
    }

    protected final static String TAG = "confirm_list_dialog";

    private String dialogText;
    private SelectionMode selectionMode;
    private Dialog dlg;
    private OnCancelListener onCancelListener;
    private OnClickListener onYesListener;
    private ConfirmListDialogDefaultAdapter<T> adapter;

    abstract protected OnClickListener createOnYesListener(AbstractConfirmListDialog dlg);

    /** rebuilds list of objects from json and does listView.setAdapter(YourAdapter(theObjectList)) */
    abstract public List<T> deserializeData(String listDataInJSON);

    public AbstractConfirmListDialog() {
        super(TAG, R.layout.dialog_confirm_list);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    SelectionMode getSelectionMode() {
        return selectionMode;
    }

    void prepareArguments(int dialogIcon,
                          String dialogTitle,
                          String dialogText,
                          String listDataInJSON,
                          SelectionMode selectionMode) {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_DIALOG_TITLE, dialogTitle);
        bundle.putString(BUNDLE_KEY_DIALOG_TEXT, dialogText);
        bundle.putString(BUNDLE_KEY_LIST_DATA, listDataInJSON);
        bundle.putInt(BUNDLE_KEY_SELECTION_MODE, selectionMode.ordinal());
        this.selectionMode = selectionMode;

        if (selectionMode == SelectionMode.SINGLE_SELECTION) {
            bundle.putInt(BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX, 0);
        }

        setArguments(bundle);
    }

    @Override
    protected void initComponents(Dialog dlg, Bundle savedInstanceState) {
        this.dlg = dlg;
        Bundle bundle = getArguments();

        String title = bundle.getString(BUNDLE_KEY_DIALOG_TITLE);

        TextView dialogTitle = findView(dlg, R.id.dialog_confirm_list_title);
        dialogTitle.setText(title);

        initListViewAndAdapter(bundle);
        initSelectAllCheckbox();
        initButtonListeners();
    }

    private void initButtonListeners() {
        final Dialog dialog = dlg;
        Button noButton = findView(dialog, R.id.dialog_confirm_list_button_no);
        noButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onCancelListener != null) {
                    onCancelListener.onCancel(dialog);
                }
                dialog.dismiss();
            }
        });

        onYesListener = createOnYesListener(this);
        if (onYesListener != null) {
            Button yesButton = findView(dialog, R.id.dialog_confirm_list_button_yes);
            yesButton.setOnClickListener(onYesListener);
        }
    }

    private void initSelectAllCheckbox() {
        final CheckBox selectAllCheckbox = findView(dlg, R.id.dialog_confirm_list_select_all_checkbox);

        if (selectionMode != SelectionMode.MULTIPLE_SELECTION) {
            selectAllCheckbox.setVisibility(View.GONE);
            return;
        }

        selectAllCheckboxOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    adapter.checkAll();
                } else {
                    adapter.clearChecked();
                }
                updateSelectedCount();
                updateSelectedInBundle();
            }
        };

        selectAllCheckbox.setVisibility(View.VISIBLE);
        selectAllCheckbox.setOnCheckedChangeListener(selectAllCheckboxOnCheckedChangeListener);
    }

    public void checkAll() {
        if (adapter != null) {
            adapter.checkAll();
            updateSelectedCount();
            updateSelectedInBundle();
        }
    }

    public abstract ConfirmListDialogDefaultAdapter<T> createAdapter(Context context,
                                                                     List<T> listData,
                                                                     SelectionMode selectionMode,
                                                                     Bundle bundle);

    private void initListViewAndAdapter(Bundle bundle) {
        ListView listView = findView(dlg, R.id.dialog_confirm_list_listview);

        String listDataString = bundle.getString(BUNDLE_KEY_LIST_DATA);
        List<T> listData = deserializeData(listDataString);

        if (selectionMode == null) {
            selectionMode = SelectionMode.fromInt(bundle.getInt(BUNDLE_KEY_SELECTION_MODE));
        }

        if (adapter == null &&
            listData != null  &&
            !listData.isEmpty()) {
            adapter = createAdapter(getActivity(), listData, selectionMode, bundle);
        } else if (adapter != null && adapter.getTotalCount() == 0 && !listData.isEmpty()) {
            adapter.addList(listData);
        }

        if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
            updateAdapterChecked(bundle);
        } else if (selectionMode == SelectionMode.SINGLE_SELECTION) {
            updateAdapterLastSelected(bundle);
            scrollToSelectedRadioButton();
        }

        if (adapter != null) {
            listView.setAdapter(adapter);
            if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
                updateSelectedCount();
            }
            adapter.setOnItemCheckedListener(this);
        }
    }

    private void updateAdapterLastSelected(Bundle bundle) {
        if (adapter != null && bundle.containsKey(BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX)) {
            int index = bundle.getInt(BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX);
            adapter.setLastSelectedRadioButton(index);
            adapter.notifyDataSetChanged();
        }
    }

    private void updateAdapterChecked(Bundle bundle) {
        if (adapter == null) {
            return;
        }
        if (bundle.containsKey(BUNDLE_KEY_CHECKED_OFFSETS)) {
            final boolean[] checkedOffsets = bundle.getBooleanArray(BUNDLE_KEY_CHECKED_OFFSETS);
            for (int i=0; i < checkedOffsets.length; i++) {
                adapter.setChecked(i, checkedOffsets[i]);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (adapter != null) {
            if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
                final Set checked = adapter.getChecked();
                if (outState != null && checked != null && !checked.isEmpty()) {
                    outState.putBooleanArray(BUNDLE_KEY_CHECKED_OFFSETS, getSelected());
                }
            } else if (selectionMode == SelectionMode.SINGLE_SELECTION) {
                outState.putInt(BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX, adapter.getLastSelectedRadioButtonIndex());
            }
        }
        super.onSaveInstanceState(outState);
    }

    void setOnYesListener(OnClickListener listener) {
        onYesListener = listener;
    }

    public OnClickListener getOnYesListener() {
        return onYesListener;
    }

    public Set<T> getChecked() {
        Set<T> result = new HashSet<>();
        if (adapter != null) {
            if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
                result = (Set<T>) adapter.getChecked();
            } else if (selectionMode == SelectionMode.SINGLE_SELECTION) {
                result.add((T) adapter.getSelectedItem());
            } else if (selectionMode == SelectionMode.NO_SELECTION) {
                result.addAll(adapter.getList());
            }
        }
        return result;
    }

    public List<T> getList() {
        List<T> result = (List<T>) Collections.EMPTY_LIST;
        if (adapter != null) {
            result = adapter.getList();
        }
        return result;
    }

    public boolean[] getSelected() {
        boolean[] result = new boolean[0];
        if (adapter != null) {
            Set<T> checked = adapter.getChecked();
            if (checked == null || checked.isEmpty()) {
                return result;
            }
            result = new boolean[adapter.getCount()];
            List<T> all = adapter.getList();
            Iterator<T> iterator = checked.iterator();
            while (iterator.hasNext()) {
                T item = iterator.next();
                int i = all.indexOf(item);
                if (i >= 0) {
                    result[i]=true;
                } else {
                    LOGGER.warn("getSelected() is not finding the checked items on the list. Verify that [" + item.getClass().getSimpleName() + "] implements equals() and hashCode()");
                }
            }
        }
        return result;
    }

    int getLastSelected() {
        return adapter.getLastSelectedRadioButtonIndex();
    }

    private void updateSelectedInBundle() {
        final Bundle arguments = getArguments();
        if (arguments != null) {
            if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
                arguments.putBooleanArray(BUNDLE_KEY_CHECKED_OFFSETS, getSelected());
            } else if (selectionMode == SelectionMode.SINGLE_SELECTION) {
                arguments.putInt(BUNDLE_KEY_LAST_SELECTED_RADIO_BUTTON_INDEX, getLastSelected());
            }
        }
    }

    private void updateSelectedCount() {
        if (adapter == null || selectionMode != SelectionMode.MULTIPLE_SELECTION) {
            return;
        }

        int selected = adapter.getCheckedCount();
        String selectedSum = adapter.getCheckedSum();
        updatedSelectedCount(selected, selectedSum);
        autoToggleSelectAllCheckbox(selected);
    }


    private void updatedSelectedCount(int selected, String selectedSum) {
        final LinearLayout summaryLayout = findView(dlg, R.id.dialog_confirm_list_selection_summary);
        final TextView numCheckedTextView = findView(dlg, R.id.dialog_confirm_list_num_checked_textview);

        boolean summaryVisible = selected > 0 &&
                                 selectionMode == SelectionMode.MULTIPLE_SELECTION &&
                                 summaryLayout != null &&
                                 numCheckedTextView != null;

        if (summaryLayout != null) {
            summaryLayout.setVisibility(View.VISIBLE);
            numCheckedTextView.setText(selected + " " + getString(R.string.selected));
            numCheckedTextView.setVisibility(View.VISIBLE);

            final TextView sumCheckedTextView = findView(dlg, R.id.dialog_confirm_list_sum_checked_textview);
            if (sumCheckedTextView != null) {
                sumCheckedTextView.setVisibility(selectedSum != null && !selectedSum.equals("") ? View.VISIBLE : View.GONE);
                if (selectedSum != null && !selectedSum.equals("")) {
                    sumCheckedTextView.setText(selectedSum);
                }
            }
        }
    }

    private void autoToggleSelectAllCheckbox(int selected) {
        // Change the state of the "Select All" checkbox only when necessary.
        final CheckBox selectAllCheckbox = findView(dlg, R.id.dialog_confirm_list_select_all_checkbox);
        selectAllCheckbox.setOnCheckedChangeListener(null);
        boolean wasChecked = selectAllCheckbox.isChecked();
        int total = adapter.getTotalCount();
        if (wasChecked && selected < total) {
            selectAllCheckbox.setChecked(false);
        } else if (!wasChecked && selected == total) {
            selectAllCheckbox.setChecked(true);
        }
        selectAllCheckbox.setOnCheckedChangeListener(selectAllCheckboxOnCheckedChangeListener);
    }

    // AbstractListAdapter.OnItemCheckedListener.onItemChecked(CompoundButton v, boolean checked)
    @Override
    public void onItemChecked(CompoundButton v, boolean checked) {
        if (selectionMode == SelectionMode.MULTIPLE_SELECTION) {
            updateSelectedCount();
        }
        updateSelectedInBundle();
        scrollToSelectedRadioButton();
    }

    private void scrollToSelectedRadioButton() {
        if (dlg != null && selectionMode == SelectionMode.SINGLE_SELECTION ) {
            ListView listView = findView(dlg, R.id.dialog_confirm_list_listview);
            if (listView == null) {
                return;
            }

            // TODO: Fix for dialog rotation, as it won't scroll if the element selected
            //       has't been painted yet. Works fine if the element would be painted from the get go
            //       then it can scroll as getChildAt() does return a view.
            // I've tried:
            // - Calculating the offset by multiplying the first visible element's height x lastSelectedIndex.
            //   it does scroll, but it doesn't populate the views, so you see blank unless you then
            //   manually scroll up and down.
            // - Populating the views with adapter.getView(), but I ended up in an infinite loop and this dialog has taken too long now.
            //   this is probably the path to fixing it. maybe you gotta turn off some listener that ends up calling
            //   this method again.

            if (listView.getAdapter() != null && listView.getChildCount() > 0) {
                View selectedView = listView.getChildAt(adapter.getLastSelectedRadioButtonIndex());

                if (selectedView == null) {
                    selectedView = adapter.getView(getLastSelected(),null,listView);
                }

                if (selectedView != null) {
                    listView.scrollTo(0, Math.max(0, (int) selectedView.getY()-(selectedView.getHeight()/2)));
                }
            }
        }
    }
}