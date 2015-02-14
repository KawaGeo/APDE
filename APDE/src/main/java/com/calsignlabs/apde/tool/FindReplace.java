package com.calsignlabs.apde.tool;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.calsignlabs.apde.APDE;
import com.calsignlabs.apde.FileMeta;
import com.calsignlabs.apde.KeyBinding;
import com.calsignlabs.apde.R;
import com.calsignlabs.apde.support.ScrollingTabContainerView;
import com.calsignlabs.apde.task.Task;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Makes the Find/Replace dialog appear.
 */
public class FindReplace implements Tool {
	public static final String PACKAGE_NAME = "com.calsignlabs.apde.tool.FindReplace";
	
	protected APDE context;
	
	protected MutableFlattenableEnum<Direction> direction;
	protected MutableFlattenableEnum<Scope> scope;
	protected MutableBoolean highlightAll;
	protected MutableBoolean wrapAround;
	protected MutableBoolean caseSensitive;
	protected MutableBoolean regExp;
	
	protected ScrollView options;
	
	protected ArrayList<FindMatch> findMatches;
	protected FindMatch lastFindMatch;

	protected EditText findTextField;
	protected EditText replaceTextField;
	
	protected ImageButton findButton;
	protected ImageButton replaceButton;
	protected ImageButton replaceAllButton;
	
	protected Paint findPaint;
	protected Paint highlightAllPaint;
	
	protected TextWatcher codeWatcher;
	
	public interface FlattenableEnum<E> {
		public String toString();
		public E fromString(String value);
	}
	
	public static enum Direction implements FlattenableEnum<Direction> {
		FORWARD, BACKWARD;
		
		@Override
		public String toString() {
			switch (this) {
			case FORWARD:
				return "forward";
			case BACKWARD:
				return "backward";
			}
			
			return "";
		}
		
		public Direction fromString(String value) {
			if (value.equals("forward"))
				return FORWARD;
			if (value.equals("backward"))
				return BACKWARD;
			
			return null;
		}
	}
	
	public static enum Scope implements FlattenableEnum<Scope> {
		SELECTION, CURRENT_TAB, ALL_TABS;
		
		@Override
		public String toString() {
			switch (this) {
			case SELECTION:
				return "selection";
			case CURRENT_TAB:
				return "currentTab";
			case ALL_TABS:
				return "allTabs";
			}
			
			return "";
		}
		
		public Scope fromString(String value) {
			if (value.equals("selection"))
				return SELECTION;
			if (value.equals("currentTab"))
				return CURRENT_TAB;
			if (value.equals("allTabs")) {
				return ALL_TABS;
			}
			
			return null;
		}
	}
	
	public static class MutableFlattenableEnum<E extends FlattenableEnum<E>> {
		private E value;
		
		public MutableFlattenableEnum() {
			value = null;
		}
		
		public MutableFlattenableEnum(E value) {
			this.value = value;
		}
		
		public static <E extends FlattenableEnum<E>> MutableFlattenableEnum<E> fromString(E inst, String value) {
			return new MutableFlattenableEnum<E>(inst.fromString(value));
		}
		
		public E get() {
			return value;
		}
		
		public void set(E value) {
			this.value = value;
		}
	}
	
	public static class MutableBoolean {
		private boolean value;
		
		public MutableBoolean() {
			value = false;
		}
		
		public MutableBoolean(boolean value) {
			this.value = value;
		}
		
		public boolean get() {
			return value;
		}
		
		public void set(boolean value) {
			this.value = value;
		}
	}
	
	@Override
	public void init(APDE context) {
		this.context = context;
		
		direction = new MutableFlattenableEnum<Direction>();
		scope = new MutableFlattenableEnum<Scope>();
		
		highlightAll = new MutableBoolean();
		wrapAround = new MutableBoolean();
		caseSensitive = new MutableBoolean();
		regExp = new MutableBoolean();
		
		findPaint = new Paint();
		findPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		findPaint.setColor(0xAA59C47F);
		
		highlightAllPaint = new Paint();
		highlightAllPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		highlightAllPaint.setColor(0x4459C47F);
		
		findMatches = new ArrayList<FindMatch>();
		
		//Used to update the find results when the code is changed
		codeWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				find(findTextField.getText().toString(), false);
			}
		};
	}
	
	@Override
	public String getMenuTitle() {
		return context.getResources().getString(R.string.find_replace);
	}
	
	@Override
	public void run() {
		if (context.getEditor().findViewById(R.id.find_replace_toolbar) != null) {
			//If it's already there, then leave it
			return;
		}
		
		//Do this on the UI thread
		context.getEditor().runOnUiThread(new Runnable() {
			public void run() {
				final LinearLayout contentView = ((LinearLayout) context.getEditor().findViewById(R.id.content));
				
				final LinearLayout findReplaceToolbar;
				
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					findReplaceToolbar = (LinearLayout) View.inflate(new ContextThemeWrapper(context, android.R.style.Theme_Holo), R.layout.find_replace_toolbar, null);
				} else {
					findReplaceToolbar = (LinearLayout) View.inflate(new ContextThemeWrapper(context, android.R.style.Theme), R.layout.find_replace_toolbar, null);
				}
				
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					options = (ScrollView) View.inflate(new ContextThemeWrapper(context, android.R.style.Theme_Holo), R.layout.find_replace_options, null);
				} else {
					options = (ScrollView) View.inflate(new ContextThemeWrapper(context, android.R.style.Theme), R.layout.find_replace_options, null);
				}
				
				findReplaceToolbar.requestLayout();
				
				findReplaceToolbar.post(new Runnable() {
					@Override
					public void run() {
						contentView.addView(findReplaceToolbar, 0, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						context.getEditor().setExtraHeaderView(findReplaceToolbar);
						context.getEditor().refreshMessageAreaLocation();
					}
				});
				
				final RelativeLayout replaceBar = (RelativeLayout) findReplaceToolbar.findViewById(R.id.find_replace_replace_bar);
				//Not visible by default
				replaceBar.setVisibility(View.GONE);
				
				ImageButton closeButton = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_close);
				final ImageButton expandCollapseButton = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_expand_collapse);
				ImageButton moreOptions = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_more_options);
				
				findButton = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_find);
				replaceButton = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_replace);
				replaceAllButton = (ImageButton) findReplaceToolbar.findViewById(R.id.find_replace_all);
				
				findTextField = (EditText) findReplaceToolbar.findViewById(R.id.find_replace_find_text);
				replaceTextField = (EditText) findReplaceToolbar.findViewById(R.id.find_replace_replace_text);
				
				RadioButton directionForward = (RadioButton) options.findViewById(R.id.find_replace_options_direction_forward);
				RadioButton directionBackward = (RadioButton) options.findViewById(R.id.find_replace_options_direction_backward);
				
				RadioButton scopeSelection = (RadioButton) options.findViewById(R.id.find_replace_options_scope_selection);
				RadioButton scopeCurrentTab = (RadioButton) options.findViewById(R.id.find_replace_options_scope_current_tab);
				RadioButton scopeAllTabs = (RadioButton) options.findViewById(R.id.find_replace_options_scope_all_tabs);
				
				//TODO Selection scope isn't currently implemented... too many issues
				scopeSelection.setVisibility(View.GONE);
				
				CheckBox highlightAllCheckBox = (CheckBox) options.findViewById(R.id.find_replace_options_highlight_all);
				CheckBox wrapAroundCheckBox = (CheckBox) options.findViewById(R.id.find_replace_options_wrap_around);
				CheckBox caseSensitiveCheckBox = (CheckBox) options.findViewById(R.id.find_replace_options_case_sensitive);
				CheckBox regExpCheckBox = (CheckBox) options.findViewById(R.id.find_replace_options_reg_exp);
				
				//TODO Regular Expressions aren't currently implemented
				regExpCheckBox.setVisibility(View.GONE);
				
				assignLongPressDescription(context, findButton, R.string.find);
				assignLongPressDescription(context, replaceButton, R.string.replace_and_find);
				assignLongPressDescription(context, replaceAllButton, R.string.replace_all);
				
				assignLongPressDescription(context, closeButton, R.string.close);
				assignLongPressDescription(context, expandCollapseButton, R.string.expand);
				
				assignEnumRadioGroup(context, "direction", 0, new RadioButton[]{directionForward, directionBackward}, new Direction[]{Direction.FORWARD, Direction.BACKWARD}, direction);
				assignEnumRadioGroup(context, "scope", 1, new RadioButton[]{scopeSelection, scopeCurrentTab, scopeAllTabs}, new Scope[]{Scope.SELECTION, Scope.CURRENT_TAB, Scope.ALL_TABS}, scope);
				
				assignBooleanCheckBox(context, "highlight_all", false, highlightAllCheckBox, highlightAll, new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							addHighlights();
						} else {
							clearHighlights();
						}
						
						//Make sure that we don't erase anything that was already there
						addSelectedFindMatchHighlight();
					}
				});
				assignBooleanCheckBox(context, "wrap_around", true, wrapAroundCheckBox, wrapAround, null);
				assignBooleanCheckBox(context, "case_sensitive", false, caseSensitiveCheckBox, caseSensitive, new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						find(findTextField.getText().toString(), false);
					}
				});
				assignBooleanCheckBox(context, "reg_exp", false, regExpCheckBox, regExp, null);
				
				closeButton.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v) {
						contentView.removeView(findReplaceToolbar);
						context.getEditor().setExtraHeaderView(null);
						context.getEditor().refreshMessageAreaLocation();

						clearHighlights();
						
						context.getCodeArea().removeTextChangedListener(codeWatcher);
					}
				});
				
				expandCollapseButton.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (replaceBar.getVisibility() == View.GONE) {
							replaceBar.setVisibility(View.VISIBLE);
							expandCollapseButton.setImageResource(R.drawable.ic_caret_up);
							
							context.getEditor().refreshMessageAreaLocation();
							
							assignLongPressDescription(context, expandCollapseButton, R.string.collapse);
						} else {
							replaceBar.setVisibility(View.GONE);
							expandCollapseButton.setImageResource(R.drawable.ic_caret_down);
							
							context.getEditor().refreshMessageAreaLocation();
							
							assignLongPressDescription(context, expandCollapseButton, R.string.expand);
						}
					}
				});
				
				findTextField.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {}
					
					@Override
					public void afterTextChanged(Editable s) {
						find(findTextField.getText().toString(), false);
					}
				});
				
				findButton.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v) {
						find(findTextField.getText().toString(), true);
					}
				});
				
				replaceButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//Replace and find
						replace(lastFindMatch, replaceTextField.getText().toString());
						find(findTextField.getText().toString(), false);
					}
				});
				
				replaceAllButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						replaceAll(findTextField.getText().toString(), replaceTextField.getText().toString());
					}
				});
				
				moreOptions.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(context.getEditor());
						builder.setTitle(R.string.find_replace);
						
						builder.setView(options);
						
						AlertDialog dialog = builder.create();
						
						dialog.setCanceledOnTouchOutside(true);
						dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								((ViewGroup) options.getParent()).removeView(options);
							}
						});
						
						dialog.show();
					}
				});
				
				findTextField.requestFocus();
				
				context.getCodeArea().addTextChangedListener(codeWatcher);
			}
		});
	}
	
	protected void assignLongPressDescription(final APDE context, final ImageButton button, final int descId) {
		button.setOnLongClickListener(new ImageButton.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Toast toast = Toast.makeText(context.getEditor(), descId, Toast.LENGTH_SHORT);
				positionToast(toast, button, context.getEditor().getWindow(), 0, 0);
				toast.show();
				
				return true;
			}
		});
	}
	
	protected void assignBooleanCheckBox(final APDE context, final String key, final boolean defaultValue, final CheckBox checkBox, final MutableBoolean value, final CompoundButton.OnCheckedChangeListener listener) {
		boolean savedValue = getPreferences(context).getBoolean(key, defaultValue);
		value.set(savedValue);
		checkBox.setChecked(savedValue);
		
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				value.set(isChecked);
				
				getPreferences(context).edit().putBoolean(key, value.get()).commit();
				
				if (listener != null) {
					listener.onCheckedChanged(buttonView, isChecked);
				}
			}
		});
	}
	
	protected <E extends FlattenableEnum<E>> void assignEnumRadioGroup(final APDE context, final String key, final int defaultValue, final RadioButton[] radioButtons, final E[] radioButtonValues, final MutableFlattenableEnum<E> value) {
		//Yes, this whole thing is WAY more complicated than it needs to be
		
		if (radioButtons.length != radioButtonValues.length) {
			return;
		}
		
		MutableFlattenableEnum<E> savedValue = MutableFlattenableEnum.fromString(radioButtonValues[0], getPreferences(context).getString(key, radioButtonValues[defaultValue].toString()));
		value.set(savedValue.get());
		
		//Determine the currently selected (saved) position
		int selectedNum = -1;
		for (int i = 0; i < radioButtonValues.length; i ++) {
			if (radioButtonValues[i].toString().equals(savedValue.get().toString())) {
				selectedNum = i;
				break;
			}
		}
		
		//Check the currently selected position
		for (int i = 0; i < radioButtons.length; i ++) {
			radioButtons[i].setChecked(i == selectedNum);
		}
		
		for (int i = 0; i < radioButtons.length; i ++) {
			//Stupid, stupid...
			final int num = i;
			
			radioButtons[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						value.set(radioButtonValues[num]);
						
						getPreferences(context).edit().putString(key, value.get().toString()).commit();
					}
				}
			});
		}
	}
	
	protected SharedPreferences getPreferences(APDE context) {
		return context.getSharedPreferences("find_replace", 0);
	}
	
	/*
	Mimic native Android action bar button long-press behavior
	
	http://stackoverflow.com/a/21026866
	 */
	public static void positionToast(Toast toast, View view, Window window, int offsetX, int offsetY) {
		// toasts are positioned relatively to decor view, views relatively to their parents, we have to gather additional data to have a common coordinate system
		Rect rect = new Rect();
		window.getDecorView().getWindowVisibleDisplayFrame(rect);
		// covert anchor view absolute position to a position which is relative to decor view
		int[] viewLocation = new int[2];
		view.getLocationInWindow(viewLocation);
		int viewLeft = viewLocation[0] - rect.left;
		int viewTop = viewLocation[1] - rect.top;
		
		// measure toast to center it relatively to the anchor view
		DisplayMetrics metrics = new DisplayMetrics();
		window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.UNSPECIFIED);
		int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.UNSPECIFIED);
		toast.getView().measure(widthMeasureSpec, heightMeasureSpec);
		int toastWidth = toast.getView().getMeasuredWidth();
		
		// compute toast offsets
		int toastX = viewLeft + (view.getWidth() / 2 - toastWidth) + offsetX;
		int toastY = viewTop + view.getHeight() + offsetY;
		
		toast.setGravity(Gravity.LEFT | Gravity.TOP, toastX, toastY);
	}
	
	public void find(final String token, final boolean advance) {
//		context.getTaskManager().launchTask("findTask", false, context.getEditor(), true, new Task() {
//			@Override
//			public void run() {
				if (token.length() == 0) {
					//Bail out
					findMatches.clear();
					clearHighlights();

					return;
				}
				
				switch (scope.get()) {
				case SELECTION:
					int selectionStart = context.getCodeArea().getSelectionStart();
					int selectionEnd = context.getCodeArea().getSelectionEnd();
					
					String selectionText = context.getCodeArea().getText().toString().substring(selectionStart, selectionEnd);
					
					findMatches = findMatches((caseSensitive.get() ? token : token.toLowerCase()), (caseSensitive.get() ? selectionText : selectionText.toLowerCase()),
							context.getEditorTabBar().getSelectedTabIndex(), selectionStart);
					
					break;
				case CURRENT_TAB:
					String currentTabText = context.getCodeArea().getText().toString();
					
					findMatches = findMatches((caseSensitive.get() ? token : token.toLowerCase()), (caseSensitive.get() ? currentTabText : currentTabText.toLowerCase()),
							context.getEditorTabBar().getSelectedTabIndex(), 0);
					
					break;
				case ALL_TABS:
					findMatches = new ArrayList<FindMatch>();
					
					for (int i = 0; i < context.getEditorTabBar().getTabCount(); i ++) {
						String tabText = context.getEditor().getTabMetas()[i].getText();
						
						findMatches.addAll(findMatches((caseSensitive.get() ? token : token.toLowerCase()), (caseSensitive.get() ? tabText : tabText.toLowerCase()), i, 0));
					}
					
					break;
				}
				
				context.getEditor().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (findMatches.size() == 0) {
							findTextField.setTextColor(context.getResources().getColor(R.color.find_replace_find_not_found));
						} else {
							findTextField.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
						}
					}
				});
				
				if (highlightAll.get()) {
					addHighlights();
				}
				
				if (advance) {
					nextFindMatch();
				} else {
					selectCursorFindMatch();
				}
//			}
//			
//			@Override
//			public CharSequence getTitle() {
//				return context.getResources().getString(R.string.find);
//			}
//		});
	}
	
	public void findOne(String token, boolean advance) {
		if (token.length() == 0) {
			//Bail out
			return;
		}
		
		FindMatch findMatch = null;
		
		boolean forward = (direction.get().equals(Direction.FORWARD));
		
		int selectionStart = context.getCodeArea().getSelectionStart();
		int selectionEnd = context.getCodeArea().getSelectionEnd();
		
		int currentPos = forward ? (advance ? selectionEnd : selectionStart) : (advance ? selectionStart : selectionEnd);
		
		String caseToken = (caseSensitive.get() ? token : token.toLowerCase());
		
		int currentTab = context.getEditorTabBar().getSelectedTabIndex();
		int tabCount = context.getEditorTabBar().getTabCount();
		
		switch (scope.get()) {
		case SELECTION:
			String selectionText = context.getCodeArea().getText().toString().substring(selectionStart, selectionEnd);
			
			findMatch = findFirstMatch(caseToken, (caseSensitive.get() ? selectionText : selectionText.toLowerCase()),
					currentTab, selectionStart, currentPos - selectionStart);
			
			break;
		case CURRENT_TAB:
			String currentTabText = context.getCodeArea().getText().toString();
			
			findMatch = findFirstMatch(caseToken, (caseSensitive.get() ? currentTabText : currentTabText.toLowerCase()),
					currentTab, 0, currentPos);
			break;
		case ALL_TABS:
			String currentTabTextAll = context.getCodeArea().getText().toString();
			
			findMatch = findFirstMatch(caseToken, (caseSensitive.get() ? currentTabTextAll : currentTabTextAll.toLowerCase()),
					currentTab, 0, currentPos);
			
			if (findMatch == null) {
				for (int i = currentTab + (forward ? 1 : -1); (forward ? i < tabCount : i >= 0); i += (forward ? 1 : -1)) {
					String tabText = context.getEditor().getTabMetas()[i].getText();
					
					findMatch = findFirstMatch(caseToken, (caseSensitive.get() ? tabText : tabText.toLowerCase()), i, 0, 0);
					
					if (findMatch != null) {
						break;
					}
				}
			}
			
			if (findMatch == null) {
				for (int i = (forward ? 0 : tabCount - 1); (forward ? i < currentTab : i > currentTab); i += (forward ? 1 : -1)) {
					String tabText = context.getEditor().getTabMetas()[i].getText();
					
					findMatch = findFirstMatch(caseToken, (caseSensitive.get() ? tabText : tabText.toLowerCase()), i, 0, 0);
					
					if (findMatch != null) {
						break;
					}
				}
			}
			
			break;
		}
		
		lastFindMatch = findMatch;
		selectFindMatch(findMatch);
	}
	
	public void addHighlights() {
		context.getCodeArea().clearHighlights();
		
		for (FindMatch findMatch : findMatches) {
			if (findMatch.tabNum == context.getEditorTabBar().getSelectedTabIndex()) {
				context.getCodeArea().addHighlight(findMatch.position, findMatch.tokenLength, highlightAllPaint);
			}
		}
		
		context.getCodeArea().invalidate();
	}
	
	public void clearHighlights() {
		context.getCodeArea().clearHighlights();
		context.getCodeArea().invalidate();
	}
	
	public void nextFindMatch() {
		if (findMatches.size() == 0) {
			//There aren't any more matches
			context.getEditor().message(context.getResources().getString(R.string.find_replace_end));
			
			return;
		}
		
		ScrollingTabContainerView tabBar = context.getEditorTabBar();
		boolean forward = direction.get().equals(Direction.FORWARD);
		
		int nextFindMatch = indexOfTextPos(tabBar.getSelectedTabIndex(), forward ? context.getCodeArea().getSelectionEnd() : context.getCodeArea().getSelectionStart(), forward);
		
		if (nextFindMatch == -1 && scope.get().equals(Scope.ALL_TABS)) {
			for (int i = tabBar.getSelectedTabIndex() + (forward ? 1 : -1); (forward ? i < tabBar.getTabCount() : i >= 0); i += (forward ? 1 : -1)) {
				int matchIndex = indexOfTextPos(i, (forward ? 0 : context.getEditor().getTabMetas()[i].getText().length() - 1), forward);
				
				if (matchIndex != -1) {
					nextFindMatch = matchIndex;
					break;
				}
			}
			
			if (nextFindMatch == -1 && wrapAround.get()) {
				for (int i = (forward ? 0 : tabBar.getTabCount() - 1); (forward ? i <= tabBar.getSelectedTabIndex() : i >= tabBar.getSelectedTabIndex()); i += (forward ? 1 : -1)) {
					int matchIndex = indexOfTextPos(i, (forward ? 0 : context.getEditor().getTabMetas()[i].getText().length() - 1), forward);
					
					if (matchIndex != -1) {
						nextFindMatch = matchIndex;
						break;
					}
				}
			}
		} else if (nextFindMatch == -1 && wrapAround.get()) {
			nextFindMatch = forward ? 0 : findMatches.size() - 1;
		}
		
		if (nextFindMatch == -1) {
			//There aren't any more matches
			context.getEditor().message(context.getResources().getString(R.string.find_replace_end));
			
			return;
		}
		
		lastFindMatch = findMatches.get(nextFindMatch);
		selectFindMatch(lastFindMatch);
		
		final int finalNextFindMatch = nextFindMatch;
		
		context.getEditor().message((finalNextFindMatch + 1) + " / " + findMatches.size());
	}
	
	public void selectCursorFindMatch() {
		if (findMatches.size() == 0) {
			//Bail out
			return;
		}
		
		ScrollingTabContainerView tabBar = context.getEditorTabBar();
		boolean forward = direction.get().equals(Direction.FORWARD);
		
		int cursorFindMatch = indexOfTextPos(tabBar.getSelectedTabIndex(), forward ? context.getCodeArea().getSelectionStart() : context.getCodeArea().getSelectionEnd(), forward);
		
		if (cursorFindMatch == -1 && wrapAround.get()) {
			cursorFindMatch = forward ? 0 : findMatches.size() - 1;
		}
		
		if (cursorFindMatch == -1) {
			//There aren't any more matches
			context.getEditor().message(context.getResources().getString(R.string.find_replace_end));
			
			return;
		}
		
		lastFindMatch = findMatches.get(cursorFindMatch);
		selectFindMatch(lastFindMatch);
	}
	
	protected int indexOfTextPos(int tabNum, int textPos, boolean forward) {
		for (int i = (forward ? 0 : findMatches.size() - 1); forward ? (i < findMatches.size()) : (i >= 0); i += (forward ? 1 : -1)) {
			FindMatch findMatch = findMatches.get(i);
			
			if (findMatch.tabNum == tabNum && (forward ? findMatch.position >= textPos : findMatch.position < textPos)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public void selectFindMatch(int findMatchNum) {
		if (findMatchNum < 0 || findMatchNum >= findMatches.size()) {
			//Bail out
			return;
		}
		
		FindMatch findMatch = findMatches.get(findMatchNum);
		
		selectFindMatch(findMatch);
	}
	
	public void selectFindMatch(final FindMatch findMatch) {
		//Select the correct tab
		if (context.getEditorTabBar().getSelectedTabIndex() != findMatch.tabNum) {
			context.getEditorTabBar().selectTab(findMatch.tabNum);
		}
		
		if (!highlightAll.get()) {
			//If highlightAll is off, then the old highlight will still be there
			context.getCodeArea().clearHighlights();
		}
		
		context.getCodeArea().addHighlight(getCurrentFindStart(), findMatch.tokenLength, findPaint);
		context.getCodeArea().invalidate();
		
		context.getCodeArea().setSelection(getCurrentFindStart(), getCurrentFindEnd());
		
		context.getCodeArea().post(new Runnable() {
			@Override
			public void run() {
				context.getCodeArea().scrollToChar(getCurrentFindStart(), context.getEditor());
			}
		});
	}
	
	public void addSelectedFindMatchHighlight() {
		if (lastFindMatch == null) {
			//Bail out
			return;
		}
		
		if (!highlightAll.get()) {
			//If highlightAll is off, then the old highlight will still be there
			context.getCodeArea().clearHighlights();
		}
		context.getCodeArea().addHighlight(getCurrentFindStart(), lastFindMatch.tokenLength, findPaint);
		context.getCodeArea().invalidate();
	}
	
	public int getCurrentFindStart() {
		return lastFindMatch != null ? lastFindMatch.textOffset + lastFindMatch.position : 0;
	}
	
	public int getCurrentFindEnd() {
		return lastFindMatch != null ? lastFindMatch.textOffset + lastFindMatch.position + lastFindMatch.tokenLength : 0;
	}
	
	public ArrayList<FindMatch> findMatches(String token, String text, int tabNum, int textOffset) {
		ArrayList<FindMatch> matches = new ArrayList<FindMatch>();
		
		int pos = 0;
		
		while (true) {
			pos = text.indexOf(token, pos);
			
			if (pos == -1) {
				break;
			}
			
			matches.add(new FindMatch(tabNum, textOffset, pos, token.length()));
			
			pos += token.length();
		}
		
		return matches;
	}
	
	public FindMatch findFirstMatch(String token, String text, int tabNum, int textOffset, int startAt) {
		int pos = text.indexOf(token, startAt);
		
		if (pos == -1) {
			return null;
		}
		
		return new FindMatch(tabNum, textOffset, pos, token.length());
	}
	
	public void replace(FindMatch findMatch, String replace) {
		if (findMatches.size() == 0) {
			//Bail out
			return;
		}
		
		context.getCodeArea().getText().replace(findMatch.position, findMatch.position + findMatch.tokenLength, replace);
	}
	
	public void replaceAll(final String find, final String replace) {
		context.getTaskManager().launchTask("replaceAllTask", false, context.getEditor(), true, new Task() {
			@Override
			public void run() {
				int count = 0;
				
				final String outputText;
				Editable[] tabTexts;
				
				switch (scope.get()) {
				case SELECTION:
					//TODO Selection scope isn't currently implemented
//					int selectionStart = context.getCodeArea().getSelectionStart();
//					int selectionEnd = context.getCodeArea().getSelectionEnd();
//					
//					Editable selectionText = Editable.Factory.getInstance().newEditable(context.getCodeArea().getText().toString().substring(selectionStart, selectionEnd));
//					
//					while (true) {
//						FindMatch selectionFindMatch = findFirstMatch(find, selectionText.toString(), context.getEditorTabBar().getSelectedTabIndex(), selectionStart);
//						
//						if (selectionFindMatch == null) {
//							break;
//						}
//						
//						context.getCodeArea().getText().replace(selectionFindMatch.position, selectionFindMatch.position + selectionFindMatch.tokenLength, replace);
//						count ++;
//					}

					outputText = context.getCodeArea().getText().toString();
					tabTexts = null;
					
					break;
				case CURRENT_TAB:
					Editable currentTabText = Editable.Factory.getInstance().newEditable(context.getCodeArea().getText());
					
					while (true) {
						FindMatch currentTabFindMatch = findFirstMatch(find, currentTabText.toString(), context.getEditorTabBar().getSelectedTabIndex(), 0, 0);
						
						if (currentTabFindMatch == null) {
							break;
						}
						
						currentTabText.replace(currentTabFindMatch.position, currentTabFindMatch.position + currentTabFindMatch.tokenLength, replace);
						count ++;
					}
					
					outputText = currentTabText.toString();
					tabTexts = null;
					
					break;
				case ALL_TABS:
					int tabNum = 0;
					
					tabTexts = new Editable[context.getEditorTabBar().getTabCount()];
					
					for (int i = 0; i < tabTexts.length; i ++) {
						if (i == context.getEditorTabBar().getSelectedTabIndex()) {
							tabTexts[i] = Editable.Factory.getInstance().newEditable(context.getCodeArea().getText());
						} else {
							tabTexts[i] = Editable.Factory.getInstance().newEditable(context.getEditor().getTabMetas()[i].getText());
						}
					}
					
					while (true) {
						FindMatch tabFindMatch = findFirstMatch(find, tabTexts[tabNum].toString(), tabNum, 0, 0);
						
						if (tabFindMatch == null) {
							if (tabNum >= context.getEditorTabBar().getTabCount() - 1) {
								break;
							} else {
								tabNum ++;
								continue;
							}
						}
						
						tabTexts[tabNum].replace(tabFindMatch.position, tabFindMatch.position + tabFindMatch.tokenLength, replace);
						count ++;
					}
					
					//Ignored
					outputText = context.getCodeArea().getText().toString();
					
					break;
				default:
					outputText = context.getCodeArea().getText().toString();
					tabTexts = null;
				}
				
				final int finalCount = count;
				final Editable[] finalOutputTexts = tabTexts;
				
				context.getEditor().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (scope.get().equals(Scope.ALL_TABS)) {
							for (int i = 0; i < finalOutputTexts.length; i ++) {
								if (i == context.getEditorTabBar().getSelectedTabIndex()) {
									context.getCodeArea().setUpdateText(finalOutputTexts[i].toString());
								} else {
									FileMeta fileMeta = context.getEditor().getTabMetas()[i];
									
									if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_key_undo_redo", true)) {
										fileMeta.update(context.getEditor(), getFileChange(fileMeta, finalOutputTexts[i]));
									}
								}
							}
						} else {
							context.getCodeArea().setUpdateText(outputText);
						}
						
						postStatus(String.format(Locale.US, context.getResources().getQuantityString(R.plurals.find_replace_replace_all_complete, finalCount), finalCount));

						clearHighlights();
					}
				});
			}
			
			@Override
			public CharSequence getTitle() {
				return context.getResources().getString(R.string.replace_all);
			}
		});
	}
	
	protected FileMeta.FileChange getFileChange(FileMeta meta, CharSequence newText) {
		FileMeta.FileChange fileChange = new FileMeta.FileChange();
		
		FileMeta.getTextChange(fileChange, meta.getText(), newText.toString());
		
		fileChange.beforeSelectionStart = meta.getSelectionStart();
		fileChange.beforeSelectionEnd = meta.getSelectionEnd();
		
		fileChange.afterSelectionStart = 0;
		fileChange.afterSelectionEnd = 0;
		
		fileChange.beforeScrollX = meta.getScrollX();
		fileChange.beforeScrollY = meta.getScrollY();
		
		fileChange.afterScrollX = 0;
		fileChange.afterScrollY = 0;
		
		return fileChange;
	}
	
	public static class FindMatch {
		public int tabNum;
		public int textOffset;
		public int position;
		public int tokenLength;
		
		public FindMatch(int tabNum, int textOffset, int position, int tokenLength) {
			this.tabNum = tabNum;
			this.textOffset = textOffset;
			this.position = position;
			this.tokenLength = tokenLength;
		}
	}
	
	@Override
	public KeyBinding getKeyBinding() {
		return context.getEditor().getKeyBindings().get("find_replace");
	}
	
	@Override
	public boolean showInToolsMenu(APDE.SketchLocation sketchLocation) {
		return true;
	}
	
	@Override
	public boolean createSelectionActionModeMenuItem(MenuItem convert) {
		return false;
	}
}
