//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuOption
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.util.Settings;
import nl.colorize.util.swing.Popups;

/**
 * A menu option is a multifunctional component for editing settings. It can use
 * both toggle buttons (+ and -) or have a single button for editing the value
 * via a pop-up window. This class can only change public fields which are 
 * accessed through reflection.
 */
public class UIMenuOption extends UIMenuWidget {
	
	private String label;
	private Object targetObject;
	private String targetField;
	private Object value;
	private String formattedValue;
	
	private List<ActionListener> listeners;
	
	private UIMenuKeyValueLabel keyValueLabel;
	private UIMenuButton plusButton;
	private UIMenuButton minusButton;
	private UIMenuButton editButton;
	
	private static final int LABEL_WIDTH = WIDGET_WIDTH - 80;
	private static final int SPACING =  10;
	
	public enum Buttons {
		PLUSMIN,
		EDIT,
		NONE
	}
	
	/**
	 * Creates a new {@code UIMenuOption} that is not bound to a property. The
	 * handling of events from this widget must be done manually.
	 * @param label The text label that will be displayed in front of the value.
	 * @param value The intially displayed value.
	 * @param useEditButton Shows a single 'edit' button, or '+' and '_', or none.
	 */
	public UIMenuOption(String label, Object value, Buttons buttons) {
		
		super(8, 8, false);
		
		this.label = label;
		this.value = value;
		this.formattedValue = (value != null) ? value.toString() : "";
		
		listeners = new ArrayList<ActionListener>();
	
		keyValueLabel = new UIMenuKeyValueLabel();
		
		int startX = LABEL_WIDTH + SPACING;
		
		switch (buttons) {
			case EDIT :
				editButton = new UIMenuButton(Settings.getInstance().getText("menu.edit"),
						UIMenuButton.ButtonType.EDIT);
				editButton.getWidget().setPosition(startX, 0);
				break;
			case PLUSMIN :
				plusButton = new UIMenuButton("+", UIMenuButton.ButtonType.OPTION);
				plusButton.getWidget().setPosition(startX, 0);
				startX += UIMenuButton.OPTION_WIDTH + SPACING;
				minusButton = new UIMenuButton("-", UIMenuButton.ButtonType.OPTION);
				minusButton.getWidget().setPosition(startX, 0);
				break;
			case NONE :
				break;
			default :
				throw new IllegalStateException();
		}
		
		repaint();
	}
	
	/**
	 * Creates a new {@code UIMenuOption} that is bound to the setting with the
	 * specified name. This property must be a public field of the Settings class.
	 */
	public UIMenuOption(String label, String setting, Class<?> datatype) {
		
		this(label, "", (datatype == String.class) ? Buttons.EDIT : Buttons.PLUSMIN);
		
		targetObject = Settings.getInstance();
		targetField = setting;
		setValue(getValueReflection());
		repaint();
		
		if (datatype == String.class) {
			editButton.getWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source, int clickX, int clickY) {
					String newValue = Popups.inputMessage(null, getLabel(), (String) getValue());
					if (newValue != null) {
						setValue(newValue);
						fireActionListeners(source, clickX, clickY);
					}
				}
			});
		}
		
		if (datatype == Boolean.class) {
			plusButton.getWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source, int clickX, int clickY) {
					boolean oldValue = (Boolean) getValue();
					setValue(!oldValue);
					fireActionListeners(source, clickX, clickY);
				}
			});
			
			minusButton.getWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source, int clickX, int clickY) {
					boolean oldValue = (Boolean) getValue();
					setValue(!oldValue);
					fireActionListeners(source, clickX, clickY);
				}
			});
		}
	}
	
	/**
	 * Creates a new {@code UIMenuOption} that is bound to the setting with the
	 * specified name. This property must be an integer, and will be kept between
	 * the requested minimum and maximum values.
	 */
	public UIMenuOption(String label, String setting, final int min, final int max,
			final String[] valueLabels) {
	
		this(label, "", Buttons.PLUSMIN);
		
		targetObject = Settings.getInstance();
		targetField = setting;
		int oldSetting = (Integer) getValueReflection();
		setValue(getValueReflection(), (valueLabels == null) ? "" + oldSetting : valueLabels[oldSetting]);
		repaint();
		
		plusButton.getWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				int oldSetting = (Integer) getValue();
				int newSetting = Math.min(oldSetting + 1, max);
				setValue(newSetting, (valueLabels == null) ? "" + newSetting : valueLabels[newSetting]);
				fireActionListeners(source, clickX, clickY);
			}
		});
		
		minusButton.getWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				int oldSetting = (Integer) getValue();
				int newSetting = Math.max(oldSetting - 1, min);
				setValue(newSetting, (valueLabels == null) ? "" + newSetting : valueLabels[newSetting]);
				fireActionListeners(source, clickX, clickY);
			}
		});
	}
	
	/** {@inheritDoc} */
	@Override
	public List<UIMenuWidget> getAdditionalWidgets() {
		List<UIMenuWidget> list = super.getAdditionalWidgets();
		list.add(keyValueLabel);
		if (plusButton != null) { list.add(plusButton); }
		if (minusButton != null) { list.add(minusButton); }
		if (editButton != null) { list.add(editButton); }
		return list;
	}
	
	/**
	 * Returns the name of the property's key label.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Changes the value, and attempts to auto-change the formatted value.
	 */
	private void setValue(Object value) {
		
		String fv = (value != null) ? value.toString() : "";
		
		if (value instanceof Boolean) {
			fv=toBooleanText((Boolean) value);
		}
		
		setValue(value, fv);
	}
	
	/**
	 * Changes the value and the formatted value at the same time. The component
	 * will be repainted after changing the values.
	 */
	private void setValue(Object value, String formattedValue) {
	
		this.value = value;
		this.formattedValue = formattedValue;
		setValueReflection(value);
		keyValueLabel.repaint();
	}
	
	/**
	 * Sets the value of the bound property to the specified value.
	 * @throws RuntimeException if the reflection fails.
	 */
	private void setValueReflection(Object value) {
	
		try {
			Field field = targetObject.getClass().getField(targetField);
			
			if (value instanceof Integer) {
				field.setInt(targetObject, (Integer) value);
			} else {
				if (value instanceof Boolean) {
					field.setBoolean(targetObject, (Boolean) value);
				} else {
					field.set(targetObject, value);
				}
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Changes only the displayed value, but not the 'real' value.
	 */
	public void setDisplayedValue(String v) {
		formattedValue = v;
		repaint();
	}
	
	/**
	 * Returns the property's current value.
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Returns the value of the bound property.
	 * @throws RuntimeException if the reflection fails.
	 */
	private Object getValueReflection() {
	
		try {
			return targetObject.getClass().getField(targetField).get(targetObject);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the property's value in formatted form. For strings this will return
	 * the original object, for integers, floats and booleans the string form.
	 */
	public String getFormattedValue() {
		return (formattedValue == null) ? "" : formattedValue;
	}
	
	/**
	 * Repaints this component as well as all sub-components. 
	 */
	@Override
	public void repaint() {
		super.repaint();
		for (UIMenuWidget i : getAdditionalWidgets()) {
			i.repaint();
		}
	}
	
	/**
	 * Empty method, since the menu option contains no graphics of its own.
	 */
	@Override
	protected void paintImage(Graphics2D g2) { 
		
	}
	
	/**
	 * Returns the height of the key/value label and a width of 0. This should
	 * be enough to properly layout this component.
	 */
	@Override
	public Dimension getHitArea() {
		return new Dimension(0, LINE_HEIGHT);
	}
	
	/**
	 * Sets the position of the key/value label and buttons to the specified
	 * coordinates.
	 */
	@Override
	public void setPosition(int x, int y) {
		
		int deltaX = x-getWidget().getX();
		
		super.setPosition(x, y);
		
		keyValueLabel.setPosition(x, y);
		if (plusButton != null) { 
			plusButton.setPosition(plusButton.getWidget().getX() + deltaX, y);
		}
		if (minusButton != null) {
			minusButton.setPosition(minusButton.getWidget().getX() + deltaX, y);
		}
		if (editButton != null) {
			editButton.setPosition(editButton.getWidget().getX() + deltaX, y);
		}
	}
	
	/**
	 * Adds the specified {@code ActionListener}. The listener will be notified of
	 * actions for the plus, minus, and edit buttons.
	 */
	public void addActionListener(ActionListener al) {
		listeners.add(al);
	}
	
	/**
	 * Removes the specified {@code ActionListener}.
	 */
	public void removeActionListener(ActionListener al) {
		listeners.remove(al);
	}
	
	/**
	 * Fires an event for all attached {@code ActionListener}s.
	 * @param source The widget that triggered the event.
	 */
	private void fireActionListeners(UIWidget source, int clickX, int clickY) {
		for (ActionListener i : listeners) {
			i.actionPerformed(source, clickX, clickY);
		}
	}
	
	/**
	 * Sets if this widget is editable. If not editable, the plus, minus and
	 * edit buttons will not be shown.
	 */
	public void setEditable(boolean editable) {
		if (plusButton != null) { plusButton.getWidget().setVisible(editable); }
		if (minusButton != null) { minusButton.getWidget().setVisible(editable); }
		if (editButton != null) { editButton.getWidget().setVisible(editable); }
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		keyValueLabel.setEnabled(enabled);
		if (plusButton != null) { plusButton.setEnabled(enabled); }
		if (minusButton != null) { minusButton.setEnabled(enabled); }
		if (editButton != null) { editButton.setEnabled(enabled); }
	}
	
	@Deprecated
	public UIMenuButton getPlusButton() {
		return plusButton;
	}
	
	@Deprecated
	public UIMenuButton getMinusButton() {
		return minusButton;
	}
	
	@Deprecated
	public UIMenuButton getEditButton() {
		return editButton;
	}
	
	/**
	 * Returns a textual form for the specified boolean value. The returned string
	 * will be in the current application locale.
	 */
	
	public static String toBooleanText(Boolean b) {
		Settings settings = Settings.getInstance();
		return b ? settings.getText("menu.yes") : settings.getText("menu.no"); 
	}

	/**
	 * Private class that displays the key and value labels for the enclosing
	 * {@code UIMenuOption}.
	 */
	private class UIMenuKeyValueLabel extends UIMenuWidget {
		
		public UIMenuKeyValueLabel() {
			super(WIDGET_TEXTURE_WIDTH, WIDGET_TEXTURE_HEIGHT, false);
			getWidget().setPainter(this);
		}

		@Override
		protected void paintImage(Graphics2D g2) {
			if (isEnabled()) {
				paintBackgroundGradient(g2, LABEL_WIDTH / 2, 0, LABEL_WIDTH / 2, LINE_HEIGHT);
			} else {
				paintDisabledGradient(g2, LABEL_WIDTH / 2, 0, LABEL_WIDTH / 2, LINE_HEIGHT);
			}
			g2.setColor(isEnabled() ? STROKE_COLOR : DISABLED_FOREGROUND_COLOR);
			paintBackgroundStroke(g2, LABEL_WIDTH / 2, 0, LABEL_WIDTH / 2, LINE_HEIGHT);
			g2.setFont(FONT);
			g2.setColor(FONT_COLOR);
			g2.drawString(getLabel(), 0, LINE_HEIGHT - BASELINE);
			g2.setColor(isEnabled() ? FONT_COLOR : DISABLED_FOREGROUND_COLOR);
			g2.drawString(getFormattedValue(), LABEL_WIDTH / 2 + SPACING, LINE_HEIGHT - BASELINE);
		}
		
		@Override
		public Dimension getHitArea() {
			return new Dimension(LABEL_WIDTH, LINE_HEIGHT);
		}
	}
}
