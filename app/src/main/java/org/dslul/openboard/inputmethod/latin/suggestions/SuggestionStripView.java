/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.suggestions;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.dslul.openboard.inputmethod.accessibility.AccessibilityUtils;
import org.dslul.openboard.inputmethod.keyboard.Keyboard;
import org.dslul.openboard.inputmethod.keyboard.KeyboardSwitcher;
import org.dslul.openboard.inputmethod.keyboard.MainKeyboardView;
import org.dslul.openboard.inputmethod.keyboard.MoreKeysPanel;
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.SuggestedWords;
import org.dslul.openboard.inputmethod.latin.SuggestedWords.SuggestedWordInfo;
import org.dslul.openboard.inputmethod.latin.ciphers.BaconianCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.CaesarCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.EnigmaCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.MessageCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.MorseCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.QuagmireCipher;
import org.dslul.openboard.inputmethod.latin.common.Constants;
import org.dslul.openboard.inputmethod.latin.define.DebugFlags;
import org.dslul.openboard.inputmethod.latin.settings.Settings;
import org.dslul.openboard.inputmethod.latin.settings.SettingsValues;
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils;
import org.dslul.openboard.inputmethod.latin.suggestions.MoreSuggestionsView.MoreSuggestionsListener;

import java.util.ArrayList;

import androidx.core.view.ViewCompat;

public final class SuggestionStripView extends RelativeLayout implements OnClickListener,
        OnLongClickListener {
    public interface Listener {
        void pickSuggestionManually(SuggestedWordInfo word);
        void onCodeInput(int primaryCode, int x, int y, boolean isKeyRepeat);
        void onTextInput(final String rawText);
        CharSequence getSelection();
    }

    static final boolean DBG = DebugFlags.DEBUG_ENABLED;
    private static final float DEBUG_INFO_TEXT_SIZE_IN_DIP = 6.0f;

    private final ViewGroup mSuggestionsStrip;
    private final ImageButton mVoiceKey;
    private final ImageButton mClipboardKey;
    private final Button mCipherKey;
    private final ImageButton mOtherKey;
    MainKeyboardView mMainKeyboardView;

    private final View mMoreSuggestionsContainer;
    private final MoreSuggestionsView mMoreSuggestionsView;
    private final MoreSuggestions.Builder mMoreSuggestionsBuilder;

    private final ArrayList<TextView> mWordViews = new ArrayList<>();
    private final ArrayList<TextView> mDebugInfoViews = new ArrayList<>();
    private final ArrayList<View> mDividerViews = new ArrayList<>();

    Listener mListener;
    private SuggestedWords mSuggestedWords = SuggestedWords.getEmptyInstance();
    private int mStartIndexOfMoreSuggestions;

    private final SuggestionStripLayoutHelper mLayoutHelper;
    private final StripVisibilityGroup mStripVisibilityGroup;

    private static class StripVisibilityGroup {
        private final View mSuggestionStripView;
        private final View mSuggestionsStrip;

        public StripVisibilityGroup(final View suggestionStripView,
                final ViewGroup suggestionsStrip) {
            mSuggestionStripView = suggestionStripView;
            mSuggestionsStrip = suggestionsStrip;
            showSuggestionsStrip();
        }

        public void setLayoutDirection(final boolean isRtlLanguage) {
            final int layoutDirection = isRtlLanguage ? ViewCompat.LAYOUT_DIRECTION_RTL
                    : ViewCompat.LAYOUT_DIRECTION_LTR;
            ViewCompat.setLayoutDirection(mSuggestionStripView, layoutDirection);
            ViewCompat.setLayoutDirection(mSuggestionsStrip, layoutDirection);
        }

        public void showSuggestionsStrip() {
            mSuggestionsStrip.setVisibility(VISIBLE);
        }

    }

    /**
     * Construct a {@link SuggestionStripView} for showing suggestions to be picked by the user.
     * @param context
     * @param attrs
     */
    public SuggestionStripView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.suggestionStripViewStyle);
    }

    public SuggestionStripView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.suggestions_strip, this);

        mSuggestionsStrip = findViewById(R.id.suggestions_strip);
        mVoiceKey = findViewById(R.id.suggestions_strip_voice_key);
        mClipboardKey = findViewById(R.id.suggestions_strip_clipboard_key);
        mCipherKey = findViewById(R.id.suggestions_strip_cipher_key);
        mOtherKey = findViewById(R.id.suggestions_strip_other_key);
        mStripVisibilityGroup = new StripVisibilityGroup(this, mSuggestionsStrip);

        for (int pos = 0; pos < SuggestedWords.MAX_SUGGESTIONS; pos++) {
            final TextView word = new TextView(context, null, R.attr.suggestionWordStyle);
            word.setContentDescription(getResources().getString(R.string.spoken_empty_suggestion));
            word.setOnClickListener(this);
            word.setOnLongClickListener(this);
            mWordViews.add(word);
            final View divider = inflater.inflate(R.layout.suggestion_divider, null);
            mDividerViews.add(divider);
            final TextView info = new TextView(context, null, R.attr.suggestionWordStyle);
            info.setTextColor(Color.WHITE);
            info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEBUG_INFO_TEXT_SIZE_IN_DIP);
            mDebugInfoViews.add(info);
        }

        mLayoutHelper = new SuggestionStripLayoutHelper(
                context, attrs, defStyle, mWordViews, mDividerViews, mDebugInfoViews);

        mMoreSuggestionsContainer = inflater.inflate(R.layout.more_suggestions, null);
        mMoreSuggestionsView = mMoreSuggestionsContainer
                .findViewById(R.id.more_suggestions_view);
        mMoreSuggestionsBuilder = new MoreSuggestions.Builder(context, mMoreSuggestionsView);

        final Resources res = context.getResources();
        mMoreSuggestionsModalTolerance = res.getDimensionPixelOffset(
                R.dimen.config_more_suggestions_modal_tolerance);
        mMoreSuggestionsSlidingDetector = new GestureDetector(
                context, mMoreSuggestionsSlidingListener);

        final TypedArray keyboardAttr = context.obtainStyledAttributes(attrs,
                R.styleable.Keyboard, defStyle, R.style.SuggestionStripView);
        final Drawable iconVoice = keyboardAttr.getDrawable(R.styleable.Keyboard_iconShortcutKey);
        final Drawable iconIncognito = keyboardAttr.getDrawable(R.styleable.Keyboard_iconIncognitoKey);
        final Drawable iconClipboard = keyboardAttr.getDrawable(R.styleable.Keyboard_iconClipboardNormalKey);
        keyboardAttr.recycle();
        mVoiceKey.setImageDrawable(iconVoice);
        mVoiceKey.setOnClickListener(this);
        mClipboardKey.setImageDrawable(iconClipboard);
        mClipboardKey.setOnClickListener(this);
        mClipboardKey.setOnLongClickListener(this);
        mCipherKey.setOnClickListener(this);

        mOtherKey.setImageDrawable(iconIncognito);
    }

    /**
     * A connection back to the input method.
     * @param listener
     */
    public void setListener(final Listener listener, final View inputView) {
        mListener = listener;
        mMainKeyboardView = inputView.findViewById(R.id.keyboard_view);
    }

    public void updateVisibility(final boolean shouldBeVisible, final boolean isFullscreenMode) {
        final int visibility = shouldBeVisible ? VISIBLE : (isFullscreenMode ? GONE : INVISIBLE);
        setVisibility(visibility);
        final SettingsValues currentSettingsValues = Settings.getInstance().getCurrent();
        mVoiceKey.setVisibility(currentSettingsValues.mShowsVoiceInputKey ? VISIBLE : GONE);
        mClipboardKey.setVisibility(currentSettingsValues.mShowsClipboardKey ? VISIBLE : (mVoiceKey.getVisibility() == GONE ? INVISIBLE : GONE));
        mCipherKey.setVisibility(currentSettingsValues.mShowsCipherKey ? VISIBLE : GONE);
        mOtherKey.setVisibility(currentSettingsValues.mIncognitoModeEnabled ? VISIBLE : INVISIBLE);
    }

    public void setSuggestions(final SuggestedWords suggestedWords, final boolean isRtlLanguage) {
        clear();
        mStripVisibilityGroup.setLayoutDirection(isRtlLanguage);
        mSuggestedWords = suggestedWords;
        mStartIndexOfMoreSuggestions = mLayoutHelper.layoutAndReturnStartIndexOfMoreSuggestions(
                getContext(), mSuggestedWords, mSuggestionsStrip, this);
        mStripVisibilityGroup.showSuggestionsStrip();
    }

    public void setMoreSuggestionsHeight(final int remainingHeight) {
        mLayoutHelper.setMoreSuggestionsHeight(remainingHeight);
    }

    public void clear() {
        mSuggestionsStrip.removeAllViews();
        removeAllDebugInfoViews();
        mStripVisibilityGroup.showSuggestionsStrip();
        dismissMoreSuggestionsPanel();
    }

    private void removeAllDebugInfoViews() {
        // The debug info views may be placed as children views of this {@link SuggestionStripView}.
        for (final View debugInfoView : mDebugInfoViews) {
            final ViewParent parent = debugInfoView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup)parent).removeView(debugInfoView);
            }
        }
    }

    private final MoreSuggestionsListener mMoreSuggestionsListener = new MoreSuggestionsListener() {
        @Override
        public void onSuggestionSelected(final SuggestedWordInfo wordInfo) {
            mListener.pickSuggestionManually(wordInfo);
            dismissMoreSuggestionsPanel();
        }

        @Override
        public void onCancelInput() {
            dismissMoreSuggestionsPanel();
        }
    };

    private final MoreKeysPanel.Controller mMoreSuggestionsController =
            new MoreKeysPanel.Controller() {
        @Override
        public void onDismissMoreKeysPanel() {
            mMainKeyboardView.onDismissMoreKeysPanel();
        }

        @Override
        public void onShowMoreKeysPanel(final MoreKeysPanel panel) {
            mMainKeyboardView.onShowMoreKeysPanel(panel);
        }

        @Override
        public void onCancelMoreKeysPanel() {
            dismissMoreSuggestionsPanel();
        }
    };

    public boolean isShowingMoreSuggestionPanel() {
        return mMoreSuggestionsView.isShowingInParent();
    }

    public void dismissMoreSuggestionsPanel() {
        mMoreSuggestionsView.dismissMoreKeysPanel();
    }

    @Override
    public boolean onLongClick(final View view) {
        if (view == mClipboardKey) {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0 && clipData.getItemAt(0) != null) {
                String clipString = clipData.getItemAt(0).coerceToText(getContext()).toString();
                if (clipString.length() == 1) {
                    mListener.onTextInput(clipString);
                } else if (clipString.length() > 1) {
                    //awkward workaround
                    mListener.onTextInput(clipString.substring(0, clipString.length() - 1));
                    mListener.onTextInput(clipString.substring(clipString.length() - 1));
                }
            }
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                    Constants.NOT_A_CODE, this);
            return true;
        }
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.NOT_A_CODE, this);
        return showMoreSuggestions();
    }

    boolean showMoreSuggestions() {
        final Keyboard parentKeyboard = mMainKeyboardView.getKeyboard();
        if (parentKeyboard == null) {
            return false;
        }
        final SuggestionStripLayoutHelper layoutHelper = mLayoutHelper;
        if (mSuggestedWords.size() <= mStartIndexOfMoreSuggestions) {
            return false;
        }
        final int stripWidth = getWidth();
        final View container = mMoreSuggestionsContainer;
        final int maxWidth = stripWidth - container.getPaddingLeft() - container.getPaddingRight();
        final MoreSuggestions.Builder builder = mMoreSuggestionsBuilder;
        builder.layout(mSuggestedWords, mStartIndexOfMoreSuggestions, maxWidth,
                (int)(maxWidth * layoutHelper.mMinMoreSuggestionsWidth),
                layoutHelper.getMaxMoreSuggestionsRow(), parentKeyboard);
        mMoreSuggestionsView.setKeyboard(builder.build());
        container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final MoreKeysPanel moreKeysPanel = mMoreSuggestionsView;
        final int pointX = stripWidth / 2;
        final int pointY = -layoutHelper.mMoreSuggestionsBottomGap;
        moreKeysPanel.showMoreKeysPanel(this, mMoreSuggestionsController, pointX, pointY,
                mMoreSuggestionsListener);
        mOriginX = mLastX;
        mOriginY = mLastY;
        for (int i = 0; i < mStartIndexOfMoreSuggestions; i++) {
            mWordViews.get(i).setPressed(false);
        }
        return true;
    }

    // Working variables for {@link onInterceptTouchEvent(MotionEvent)} and
    // {@link onTouchEvent(MotionEvent)}.
    private int mLastX;
    private int mLastY;
    private int mOriginX;
    private int mOriginY;
    private final int mMoreSuggestionsModalTolerance;
    private boolean mNeedsToTransformTouchEventToHoverEvent;
    private boolean mIsDispatchingHoverEventToMoreSuggestions;
    private final GestureDetector mMoreSuggestionsSlidingDetector;
    private final GestureDetector.OnGestureListener mMoreSuggestionsSlidingListener =
            new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent down, MotionEvent me, float deltaX, float deltaY) {
            final float dy = me.getY() - down.getY();
            if (deltaY > 0 && dy < 0) {
                return showMoreSuggestions();
            }
            return false;
        }
    };

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent me) {
        // Detecting sliding up finger to show {@link MoreSuggestionsView}.
        if (!mMoreSuggestionsView.isShowingInParent()) {
            mLastX = (int)me.getX();
            mLastY = (int)me.getY();
            return mMoreSuggestionsSlidingDetector.onTouchEvent(me);
        }
        if (mMoreSuggestionsView.isInModalMode()) {
            return false;
        }

        final int action = me.getAction();
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);
        if (Math.abs(x - mOriginX) >= mMoreSuggestionsModalTolerance
                || mOriginY - y >= mMoreSuggestionsModalTolerance) {
            // Decided to be in the sliding suggestion mode only when the touch point has been moved
            // upward. Further {@link MotionEvent}s will be delivered to
            // {@link #onTouchEvent(MotionEvent)}.
            mNeedsToTransformTouchEventToHoverEvent =
                    AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled();
            mIsDispatchingHoverEventToMoreSuggestions = false;
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            // Decided to be in the modal input mode.
            mMoreSuggestionsView.setModalMode();
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
        // Don't populate accessibility event with suggested words and voice key.
        return true;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent me) {
        if (!mMoreSuggestionsView.isShowingInParent()) {
            // Ignore any touch event while more suggestions panel hasn't been shown.
            // Detecting sliding up is done at {@link #onInterceptTouchEvent}.
            return true;
        }
        // In the sliding input mode. {@link MotionEvent} should be forwarded to
        // {@link MoreSuggestionsView}.
        final int index = me.getActionIndex();
        final int x = mMoreSuggestionsView.translateX((int)me.getX(index));
        final int y = mMoreSuggestionsView.translateY((int)me.getY(index));
        me.setLocation(x, y);
        if (!mNeedsToTransformTouchEventToHoverEvent) {
            mMoreSuggestionsView.onTouchEvent(me);
            return true;
        }
        // In sliding suggestion mode with accessibility mode on, a touch event should be
        // transformed to a hover event.
        final int width = mMoreSuggestionsView.getWidth();
        final int height = mMoreSuggestionsView.getHeight();
        final boolean onMoreSuggestions = (x >= 0 && x < width && y >= 0 && y < height);
        if (!onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Just drop this touch event because dispatching hover event isn't started yet and
            // the touch event isn't on {@link MoreSuggestionsView}.
            return true;
        }
        final int hoverAction;
        if (onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Transform this touch event to a hover enter event and start dispatching a hover
            // event to {@link MoreSuggestionsView}.
            mIsDispatchingHoverEventToMoreSuggestions = true;
            hoverAction = MotionEvent.ACTION_HOVER_ENTER;
        } else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
            // Transform this touch event to a hover exit event and stop dispatching a hover event
            // after this.
            mIsDispatchingHoverEventToMoreSuggestions = false;
            mNeedsToTransformTouchEventToHoverEvent = false;
            hoverAction = MotionEvent.ACTION_HOVER_EXIT;
        } else {
            // Transform this touch event to a hover move event.
            hoverAction = MotionEvent.ACTION_HOVER_MOVE;
        }
        me.setAction(hoverAction);
        mMoreSuggestionsView.onHoverEvent(me);
        return true;
    }

    @Override
    public void onClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.CODE_UNSPECIFIED, this);
        if (view == mVoiceKey) {
            mListener.onCodeInput(Constants.CODE_SHORTCUT,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false /* isKeyRepeat */);
            return;
        }
        if (view == mClipboardKey) {
            mListener.onCodeInput(Constants.CODE_CLIPBOARD,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false /* isKeyRepeat */);
            return;
        }
        if (view == mCipherKey) {
            showCipherDialog();
            return;
        }

        final Object tag = view.getTag();
        // {@link Integer} tag is set at
        // {@link SuggestionStripLayoutHelper#setupWordViewsTextAndColor(SuggestedWords,int)} and
        // {@link SuggestionStripLayoutHelper#layoutPunctuationSuggestions(SuggestedWords,ViewGroup}
        if (tag instanceof Integer) {
            final int index = (Integer) tag;
            if (index >= mSuggestedWords.size()) {
                return;
            }
            final SuggestedWordInfo wordInfo = mSuggestedWords.getInfo(index);
            mListener.pickSuggestionManually(wordInfo);
        }
    }

    private void showCipherDialog() {
        final Context context = getContext();
        final SharedPreferences prefs = Settings.getInstance().getSharedPreferences();

        final ScrollView scrollView = new ScrollView(context);
        final LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(container);
        final int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics());
        container.setPadding(padding, padding / 2, padding, 0);

        final Button closeButton = new Button(context);
        closeButton.setText(R.string.close);
        container.addView(closeButton);

        final Button caesarButton = new Button(context);
        caesarButton.setText(R.string.caesar_cipher);
        container.addView(caesarButton);

        final LinearLayout caesarSettings = new LinearLayout(context);
        caesarSettings.setOrientation(LinearLayout.VERTICAL);
        caesarSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        caesarSettings.addView(messageInput);

        final EditText shiftInput = new EditText(context);
        shiftInput.setHint(R.string.caesar_shift);
        shiftInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        shiftInput.setText(String.valueOf(prefs.getInt(Settings.PREF_CAESAR_CIPHER_SHIFT, 3)));
        caesarSettings.addView(shiftInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        caesarSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        caesarSettings.addView(decryptButton);
        container.addView(caesarSettings);

        addEnigmaPanel(context, container, prefs, false);
        addEnigmaPanel(context, container, prefs, true);
        addSimpleCipherPanel(context, container, R.string.baconian_cipher, new BaconianCipher());
        addSimpleCipherPanel(context, container, R.string.morse_code, new MorseCipher());
        addQuagmirePanel(context, container, prefs, R.string.quagmire_i, QuagmireCipher.Variant.I);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_ii, QuagmireCipher.Variant.II);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_iii, QuagmireCipher.Variant.III);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_iv, QuagmireCipher.Variant.IV);

        shiftInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveCaesarShift(prefs, shiftInput);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        caesarButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                caesarSettings.setVisibility(
                        caesarSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputCaesarText(prefs, messageInput, shiftInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputCaesarText(prefs, messageInput, shiftInput, true);
            }
        });

        final int popupHeight = getRootView() == null ? ViewGroup.LayoutParams.WRAP_CONTENT
                : getRootView().getHeight();
        final PopupWindow popupWindow = new PopupWindow(scrollView,
                ViewGroup.LayoutParams.MATCH_PARENT, popupHeight, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        popupWindow.showAtLocation(this, Gravity.BOTTOM, 0, 0);
    }



    private void addSimpleCipherPanel(final Context context, final LinearLayout container,
            final int titleResId, final MessageCipher cipher) {
        final Button cipherButton = new Button(context);
        cipherButton.setText(titleResId);
        container.addView(cipherButton);

        final LinearLayout cipherSettings = new LinearLayout(context);
        cipherSettings.setOrientation(LinearLayout.VERTICAL);
        cipherSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        cipherSettings.addView(messageInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        cipherSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        cipherSettings.addView(decryptButton);
        container.addView(cipherSettings);

        cipherButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                cipherSettings.setVisibility(
                        cipherSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputText(cipher.encrypt(messageInput.getText().toString()));
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputText(cipher.decrypt(messageInput.getText().toString()));
            }
        });
    }

    private void addQuagmirePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs, final int titleResId, final QuagmireCipher.Variant variant) {
        final Button quagmireButton = new Button(context);
        quagmireButton.setText(titleResId);
        container.addView(quagmireButton);

        final LinearLayout quagmireSettings = new LinearLayout(context);
        quagmireSettings.setOrientation(LinearLayout.VERTICAL);
        quagmireSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        quagmireSettings.addView(messageInput);

        final EditText plainKeywordInput = new EditText(context);
        plainKeywordInput.setHint(R.string.quagmire_plain_keyword_hint);
        plainKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_PLAIN_KEYWORD, "KEYWORD"));
        quagmireSettings.addView(plainKeywordInput);

        final EditText cipherKeywordInput = new EditText(context);
        cipherKeywordInput.setHint(R.string.quagmire_cipher_keyword_hint);
        cipherKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_CIPHER_KEYWORD, "CIPHER"));
        quagmireSettings.addView(cipherKeywordInput);

        final EditText indicatorKeywordInput = new EditText(context);
        indicatorKeywordInput.setHint(R.string.quagmire_indicator_keyword_hint);
        indicatorKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_INDICATOR_KEYWORD, "KEY"));
        quagmireSettings.addView(indicatorKeywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        quagmireSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        quagmireSettings.addView(decryptButton);
        container.addView(quagmireSettings);

        quagmireButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                quagmireSettings.setVisibility(
                        quagmireSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputQuagmireText(prefs, variant, messageInput, plainKeywordInput,
                        cipherKeywordInput, indicatorKeywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputQuagmireText(prefs, variant, messageInput, plainKeywordInput,
                        cipherKeywordInput, indicatorKeywordInput, true);
            }
        });
    }

    private void outputQuagmireText(final SharedPreferences prefs,
            final QuagmireCipher.Variant variant, final EditText messageInput,
            final EditText plainKeywordInput, final EditText cipherKeywordInput,
            final EditText indicatorKeywordInput, final boolean decrypt) {
        prefs.edit()
                .putString(Settings.PREF_QUAGMIRE_PLAIN_KEYWORD,
                        plainKeywordInput.getText().toString())
                .putString(Settings.PREF_QUAGMIRE_CIPHER_KEYWORD,
                        cipherKeywordInput.getText().toString())
                .putString(Settings.PREF_QUAGMIRE_INDICATOR_KEYWORD,
                        indicatorKeywordInput.getText().toString())
                .apply();
        final QuagmireCipher cipher = new QuagmireCipher(variant,
                plainKeywordInput.getText().toString(), cipherKeywordInput.getText().toString(),
                indicatorKeywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void outputText(final String output) {
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }

    private void addEnigmaPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs, final boolean m4) {
        final Button enigmaButton = new Button(context);
        enigmaButton.setText(m4 ? R.string.enigma_m4 : R.string.enigma_m3);
        container.addView(enigmaButton);

        final LinearLayout enigmaSettings = new LinearLayout(context);
        enigmaSettings.setOrientation(LinearLayout.VERTICAL);
        enigmaSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        enigmaSettings.addView(messageInput);

        final EditText thinRotorInput = new EditText(context);
        if (m4) {
            thinRotorInput.setHint(R.string.enigma_thin_rotor_hint);
            thinRotorInput.setText(prefs.getString(Settings.PREF_ENIGMA_M4_THIN_ROTOR, "Beta"));
            enigmaSettings.addView(thinRotorInput);
        }

        final EditText rotorsInput = new EditText(context);
        rotorsInput.setHint(R.string.enigma_rotors_hint);
        rotorsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_ROTORS
                : Settings.PREF_ENIGMA_M3_ROTORS, "I II III"));
        enigmaSettings.addView(rotorsInput);

        final EditText reflectorInput = new EditText(context);
        reflectorInput.setHint(R.string.enigma_reflector_hint);
        reflectorInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_REFLECTOR
                : Settings.PREF_ENIGMA_M3_REFLECTOR, m4 ? "B Thin" : "B"));
        enigmaSettings.addView(reflectorInput);

        final EditText positionsInput = new EditText(context);
        positionsInput.setHint(R.string.enigma_positions_hint);
        positionsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_POSITIONS
                : Settings.PREF_ENIGMA_M3_POSITIONS, m4 ? "AAAA" : "AAA"));
        enigmaSettings.addView(positionsInput);

        final EditText ringsInput = new EditText(context);
        ringsInput.setHint(R.string.enigma_rings_hint);
        ringsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_RINGS
                : Settings.PREF_ENIGMA_M3_RINGS, m4 ? "AAAA" : "AAA"));
        enigmaSettings.addView(ringsInput);

        final EditText plugboardInput = new EditText(context);
        plugboardInput.setHint(R.string.enigma_plugboard_hint);
        plugboardInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_PLUGBOARD
                : Settings.PREF_ENIGMA_M3_PLUGBOARD, ""));
        enigmaSettings.addView(plugboardInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        enigmaSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        enigmaSettings.addView(decryptButton);
        container.addView(enigmaSettings);

        enigmaButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                enigmaSettings.setVisibility(
                        enigmaSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputEnigmaText(prefs, m4, messageInput, thinRotorInput, rotorsInput,
                        reflectorInput, positionsInput, ringsInput, plugboardInput);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputEnigmaText(prefs, m4, messageInput, thinRotorInput, rotorsInput,
                        reflectorInput, positionsInput, ringsInput, plugboardInput);
            }
        });
    }

    private void outputEnigmaText(final SharedPreferences prefs, final boolean m4,
            final EditText messageInput, final EditText thinRotorInput, final EditText rotorsInput,
            final EditText reflectorInput, final EditText positionsInput, final EditText ringsInput,
            final EditText plugboardInput) {
        saveEnigmaSettings(prefs, m4, thinRotorInput, rotorsInput, reflectorInput, positionsInput,
                ringsInput, plugboardInput);
        final EnigmaCipher cipher = m4
                ? EnigmaCipher.createM4(thinRotorInput.getText().toString(),
                        rotorsInput.getText().toString(), reflectorInput.getText().toString(),
                        positionsInput.getText().toString(), ringsInput.getText().toString(),
                        plugboardInput.getText().toString())
                : EnigmaCipher.createM3(rotorsInput.getText().toString(),
                        reflectorInput.getText().toString(), positionsInput.getText().toString(),
                        ringsInput.getText().toString(), plugboardInput.getText().toString());
        final String output = cipher.encrypt(messageInput.getText().toString());
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }

    private void saveEnigmaSettings(final SharedPreferences prefs, final boolean m4,
            final EditText thinRotorInput, final EditText rotorsInput, final EditText reflectorInput,
            final EditText positionsInput, final EditText ringsInput, final EditText plugboardInput) {
        final SharedPreferences.Editor editor = prefs.edit();
        if (m4) {
            editor.putString(Settings.PREF_ENIGMA_M4_THIN_ROTOR,
                    thinRotorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_ROTORS, rotorsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_REFLECTOR,
                    reflectorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_POSITIONS,
                    positionsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_RINGS, ringsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_PLUGBOARD,
                    plugboardInput.getText().toString());
        } else {
            editor.putString(Settings.PREF_ENIGMA_M3_ROTORS, rotorsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_REFLECTOR,
                    reflectorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_POSITIONS,
                    positionsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_RINGS, ringsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_PLUGBOARD,
                    plugboardInput.getText().toString());
        }
        editor.apply();
    }

    private void outputCaesarText(final SharedPreferences prefs, final EditText messageInput,
            final EditText shiftInput, final boolean decrypt) {
        final CaesarCipher cipher = new CaesarCipher(saveCaesarShift(prefs, shiftInput));
        final String input = messageInput.getText().toString();
        final String output = decrypt ? cipher.decrypt(input) : cipher.encrypt(input);
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }

    private int saveCaesarShift(final SharedPreferences prefs, final EditText shiftInput) {
        int shift = 3;
        try {
            shift = Integer.parseInt(shiftInput.getText().toString());
        } catch (NumberFormatException ignored) {
        }
        prefs.edit().putInt(Settings.PREF_CAESAR_CIPHER_SHIFT, shift).apply();
        return shift;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissMoreSuggestionsPanel();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        // Called by the framework when the size is known. Show the important notice if applicable.
        // This may be overriden by showing suggestions later, if applicable.
    }
}
