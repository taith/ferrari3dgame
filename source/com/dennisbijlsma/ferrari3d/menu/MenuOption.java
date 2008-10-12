//--------------------------------------------------------------------------------
// Ferrari3D
// MenuOption
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.swing.*;

/**
 * A menu option is a multifunctional component for editing settings. It can use
 * both toggle buttons (+ and -) or have a single button for editing the value
 * via a pop-up window. This class can only change public fields which are 
 * accessed through reflection. Manual editing of the setting is also possible, 
 * for that override the matching methods.
 */

public class MenuOption extends MenuWidget {
	
	private ActionListener listener;
	
	private Object instance;
	private String field;
	private String datatype;
	
	private String label;
	private String value;
	
	private MenuButton plusButton;
	private MenuButton minusButton;
	private MenuButton editButton;
		
	public static final String ACTION_PLUS="plus";
	public static final String ACTION_MINUS="minus";
	public static final String ACTION_EDIT="edit";
	private static final String TEXT_TRUE=Settings.getInstance().getText("menu.yes");
	private static final String TEXT_FALSE=Settings.getInstance().getText("menu.no");
	
	/**
	 * Creates a <code>MenuOption</code> for manually changing the setting. There
	 * are two possible display modes, one with plus and minus buttons and the
	 * other with a single button for editing. There is a third option, to not
	 * display any buttons at all.
	 * @param label The label describing the setting.
	 * @param value The initially displayed value.
	 * @param numButtons The number of displayed buttons, can be 0, 1 or 2.
	 */
	
	public MenuOption(final String label,String value,int numButtons) {
		
		super();
		
		setLabel(label);
		setValue(value);
		
		if (numButtons==1) {
			editButton=new MenuButton("Edit") {
				public void pressed() {
					String input=PopUp.input(getRootComponent(),label,getValue());
					if (input!=null) {
						editPressed(input);
					}
				}
			};
			this.add(editButton);
		}
		
		if (numButtons==2) {
			plusButton=new MenuButton("+") {
				public void pressed() {
					plusPressed();
				}
			};
			this.add(plusButton);
			
			minusButton=new MenuButton("-") {
				public void pressed() {
					minusPressed();
				}
			};
			this.add(minusButton);
		}
	}
	
	/**
	 * Creates a <code>MenuOption</code> which automatically changes the setting
	 * through reflection. Using this constructor there is no more need to add
	 * event handlers.
	 * @param label The label describing the setting.
	 * @param instance The object containing the setting member field.
	 * @param field The field name, should be <code>public</code>.
	 * @param datatype A string describing the type of setting.
	 */
	
	public MenuOption(String label,Object instance,String field,String datatype) {
		
		this(label,"",datatype.equals("string") ? 1 : 2);
		
		this.instance=instance;
		this.field=field;
		this.datatype=datatype.toLowerCase();
		
		setValue(getSetting());
	}
	
	/**
	 * Creates a <code>MenuOption</code> which automatically changes a numerical 
	 * value through reflection.
	 * @param label The label describing the setting.
	 * @param instance The object containing the setting member field.
	 * @param field The field name, should be <code>public</code>.
	 * @param minValue The minimum value for this field.
	 * @param maxValue The maximum value for this field.
	 * @param valueLabels The labels that should be used to describe the values.
	 */
	
	public MenuOption(String label,Object instance,String field,final int minValue,final int maxValue,
			final String[] valueLabels) {
	
		this(label,"",2);
		
		if ((valueLabels!=null) && (valueLabels.length!=maxValue+1)) {
			throw new IllegalArgumentException("Invalid value labels");
		}
		
		this.instance=instance;
		this.field=field;
		this.datatype="int";
		
		setValue(valueLabels==null ? getSetting() : valueLabels[Integer.parseInt(getSetting())]);
		
		setListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource().equals(ACTION_PLUS)) {
					int oldSetting=Integer.parseInt(getSetting());
					int newSetting=Math.min(oldSetting+1,maxValue);
					setSetting(""+newSetting);
					setValue(valueLabels==null ? getSetting() : valueLabels[newSetting]);
				}
				if (e.getSource().equals(ACTION_MINUS)) {
					int oldSetting=Integer.parseInt(getSetting());
					int newSetting=Math.max(oldSetting-1,minValue);
					setSetting(""+newSetting);
					setValue(valueLabels==null ? getSetting() : valueLabels[newSetting]);
				}
			}
		});
	}
	
	/**
	 * ({@inheritDoc}
	 */
	
	public void paintWidget(Graphics2D g2) {
		
		paintBackgroundGradient(g2,getWidth()/2,0,getWidth()/2-getHeight()*2-9,getHeight());
		g2.setColor(STROKE_COLOR);
		g2.setStroke(STROKE);
		g2.drawRoundRect(getWidth()/2,0,getWidth()/2-getHeight()*2-10,getHeight()-1,ARC,ARC);
		
		String valueString=value;
		if (valueString.equals("true")) { valueString=TEXT_TRUE; }
		if (valueString.equals("false")) { valueString=TEXT_FALSE; }
		
		g2.setFont(FONT);
		g2.setColor(FONT_COLOR);
		g2.drawString(label,0,getHeight()-BASELINE);
		g2.drawString(valueString,getWidth()/2+10,getHeight()-BASELINE);
		
		if ((plusButton!=null) && (minusButton!=null)) {
			plusButton.setBounds(getWidth()-getHeight()*2-5,0,getHeight(),getHeight());
			minusButton.setBounds(getWidth()-getHeight(),0,getHeight(),getHeight());
		}
		
		if (editButton!=null) {
			editButton.setBounds(getWidth()-getHeight()*2-5,0,getHeight()*2+5,getHeight());
		}
	}
	
	/**
	 * Invoked when the plus button is pressed. Override this method when using
	 * manual changing of the setting. In other cases the component will attempt
	 * to change the setting automatically.
	 */
	
	public void plusPressed() {
		
		if ((instance!=null) && (field!=null) && (datatype.equals("boolean"))) {
			setSetting(""+!getSetting().equals("true"));
			setValue(getSetting());
		}
		
		if (listener!=null) {
			listener.actionPerformed(new ActionEvent(ACTION_PLUS,ActionEvent.ACTION_PERFORMED,"plus"));
		}
	}
	
	/**
	 * Invoked when the minus button is pressed. Override this method when using
	 * manual changing of the setting. In other cases the component will attempt
	 * to change the setting automatically.
	 */
	
	public void minusPressed() {
		
		if (listener!=null) {
			listener.actionPerformed(new ActionEvent(ACTION_MINUS,ActionEvent.ACTION_PERFORMED,"minus"));
		} else {
			if ((instance!=null) && (field!=null) && (datatype.equals("boolean"))) {
				setSetting(""+!getSetting().equals("true"));
				setValue(getSetting());
			}	
		}
	}
	
	/**
	 * Invoked when the edit button is pressed. Override this method when using
	 * manual changing of the setting. In other cases the component will attempt
	 * to change the setting automatically.
	 * @param input The string that was entered in the input dialog. 
	 */
	
	public void editPressed(String input) {
		
		if (listener!=null) {
			listener.actionPerformed(new ActionEvent(ACTION_EDIT,ActionEvent.ACTION_PERFORMED,"edit"));
		} else {
			if ((instance!=null) && (field!=null) && (datatype.equals("string"))) {
				setSetting(input);
				setValue(getSetting());
			}
		}
	}
	
	/**
	 * Changes the value of the binded setting to the specified value. The
	 * parameter will be converted to the correct data type.
	 */
	
	private void setSetting(String v) {
	
		try {
			Field f=instance.getClass().getField(field);
			if (datatype.equals("object")) { f.set(instance,v); }
			if (datatype.equals("string")) { f.set(instance,v); }
			if (datatype.equals("boolean")) { f.setBoolean(instance,v.equals("true")); }
			if (datatype.equals("int")) { f.setInt(instance,Integer.parseInt(v)); }
			if (datatype.equals("float")) { f.setFloat(instance,Float.parseFloat(v)); }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the value of the binded setting through reflection. The value
	 * is returned as a string.
	 */
	
	private String getSetting() {
		
		try {
			Field f=instance.getClass().getField(field);
			return f.get(instance).toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Changes the label describing the setting. When the parameter is not set
	 * the displayed value will be an empty string.
	 */
	
	public void setLabel(String k) {
				
		label=(k!=null) ? k : "";		
		repaint();
	}
	
	/**
	 * Returns the label describing the setting.
	 */
	
	public String getLabel() {
		
		return label;
	}
	
	/**
	 * Changes the displayed value to that of the parameter. When the parameter
	 * is not set an empty string will be displayed.
	 */
	
	public void setValue(String v) {
		
		value=(v!=null) ? v : "";
		
		repaint();
	}
	
	/**
	 * Returns the currently displayed value for the setting.
	 */
	
	public String getValue() {
	
		return value;
	}
	
	
	
	public void setListener(ActionListener listener) {
	
		this.listener=listener;
	}
	
	
	
	public ActionListener getListener() {
		
		return listener;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public Dimension getWidgetSize() {
	 
		return new Dimension(getWidth(),LINE_HEIGHT);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void setEnabled(boolean enabled) {
	
		super.setEnabled(enabled);
		
		if (plusButton!=null) { plusButton.setEnabled(enabled); }
		if (minusButton!=null) { minusButton.setEnabled(enabled); }
		if (editButton!=null) { editButton.setEnabled(enabled); }
	}
	
	/**
	 * Returns a language-specific form for 'true' or 'false'. This class uses 
	 * these strings automatically.
	 */
	
	public static String getBooleanText(boolean b) {
	
		return b ? TEXT_TRUE : TEXT_FALSE;
	}
}