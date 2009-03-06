//--------------------------------------------------------------------------------
// Ferrari3D
// UIMenuOption
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.util.swing.Popups;

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
	
	private UIMenuKeyValueLabel keyValueLabel;
	private UIMenuButton plusButton;
	private UIMenuButton minusButton;
	private UIMenuButton editButton;
	
	private static final int LABEL_WIDTH=200;
	private static final int LABEL_HEIGHT=25;
	private static final int LABEL_TEXTURE_WIDTH=256;
	private static final int LABEL_TEXTURE_HEIGHT=32;
	private static final int SPACING=10;
	
	/**
	 * Creates a new {@code UIMenuOption} that is not bound to a property. The
	 * handling of events from this widget must be done manually.
	 * @param label The text label that will be displayed in front of the value.
	 * @param useEditButton If true shows a single 'edit' button, else '+' and '_'.
	 */
	
	public UIMenuOption(String label,boolean useEditButton) {
		
		super(8,8,false);
		
		this.label=label;
	
		keyValueLabel=new UIMenuKeyValueLabel();
		
		int startX=LABEL_WIDTH+SPACING;
		
		if (useEditButton) {
			editButton=new UIMenuButton(Settings.getInstance().getText("menu.edit"),
					UIMenuButton.ButtonType.EDIT);
			editButton.getSingleWidget().setPosition(startX,0);
		} else {
			plusButton=new UIMenuButton("+",UIMenuButton.ButtonType.OPTION);
			plusButton.getSingleWidget().setPosition(startX,0);
			startX+=UIMenuButton.OPTION_WIDTH+SPACING;
			minusButton=new UIMenuButton("-",UIMenuButton.ButtonType.OPTION);
			minusButton.getSingleWidget().setPosition(startX,0);
		}
	}
	
	/**
	 * Creates a new {@code UIMenuOption} that is bound to the setting with the
	 * specified name. This property must be a public field of the Settings class.
	 */
	
	public UIMenuOption(String label,String setting,Class<?> datatype) {
		
		this(label,(datatype==String.class));
		
		targetObject=Settings.getInstance();
		targetField=setting;
		setValue(getValueReflection());
		repaintImages();
		
		if (datatype==String.class) {
			editButton.getSingleWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source) {
					String newValue=Popups.inputMessage(null,getLabel(),(String) getValue());
					if (newValue!=null) {
						setValue(newValue);
					}
				}
			});
		}
		
		if (datatype==Boolean.class) {
			plusButton.getSingleWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source) {
					boolean oldValue=(Boolean) getValue();
					setValue(!oldValue);
				}
			});
			
			minusButton.getSingleWidget().addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source) {
					boolean oldValue=(Boolean) getValue();
					setValue(!oldValue);
				}
			});
		}
	}
	
	/**
	 * Constructor for compatability with 2.0.x versions.
	 */

	@Deprecated
	public UIMenuOption(String label,String setting,String datatype) {
		
		this(label,setting,datatype.equals("string") ? String.class : Boolean.class);
	}
	
	/**
	 * Creates a new {@code UIMenuOption} that is bound to the setting with the
	 * specified name. This property must be an integer, and will be kept between
	 * the requested minimum and maximum values.
	 */
	
	public UIMenuOption(String label,String setting,final int min,final int max,
			final String[] valueLabels) {
	
		this(label,false);
		
		targetObject=Settings.getInstance();
		targetField=setting;
		int oldSetting=(Integer) getValueReflection();
		setValue(getValueReflection(),(valueLabels==null) ? ""+oldSetting : valueLabels[oldSetting]);
		repaintImages();
		
		plusButton.getSingleWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				int oldSetting=(Integer) getValue();
				int newSetting=Math.min(oldSetting+1,max);
				setValue(newSetting,(valueLabels==null) ? ""+newSetting : valueLabels[newSetting]);
			}
		});
		
		minusButton.getSingleWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				int oldSetting=(Integer) getValue();
				int newSetting=Math.max(oldSetting-1,min);
				setValue(newSetting,(valueLabels==null) ? ""+newSetting : valueLabels[newSetting]);
			}
		});
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
		
		String fv=(value!=null) ? value.toString() : "";
		
		if (value instanceof Boolean) {
			Settings s=Settings.getInstance();
			fv=((Boolean) value) ? s.getText("menu.yes") : s.getText("menu.no"); 
		}
		
		setValue(value,fv);
	}
	
	/**
	 * Changes the value and the formatted value at the same time. The component
	 * will be repainted after changing the values.
	 */
	
	private void setValue(Object value,String formattedValue) {
	
		this.value=value;
		this.formattedValue=formattedValue;
		setValueReflection(value);
		keyValueLabel.repaintImages();
	}
	
	/**
	 * Sets the value of the bound property to the specified value.
	 * @throws RuntimeException if the reflection fails.
	 */
	
	private void setValueReflection(Object value) {
	
		try {
			Field field=targetObject.getClass().getField(targetField);
			
			if (value instanceof Integer) {
				field.setInt(targetObject,(Integer) value);
			} else {
				if (value instanceof Boolean) {
					field.setBoolean(targetObject,(Boolean) value);
				} else {
					field.set(targetObject,value);
				}
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
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
			Field field=targetObject.getClass().getField(targetField);
			return field.get(targetObject);
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
	
		return (formattedValue==null) ? "" : formattedValue;
	}
	
	/**
	 * Empty method, since the menu option contains no graphics of its own.
	 */
	
	@Override
	protected void paintImage(BufferedImage image) {
		
	}
	
	/**
	 * Returns the height of the key/value label and a width of 0. This should
	 * be enough to properly layout this component.
	 */
	
	@Override
	public Dimension getHitArea() {
		
		return new Dimension(0,LABEL_HEIGHT);
	}
	
	/**
	 * Sets the position of the key/value label and buttons to the specified
	 * coordinates.
	 */
	
	@Override
	public void setPosition(int x,int y) {
		
		int deltaX=x-getSingleWidget().getX();
		
		super.setPosition(x,y);
		
		keyValueLabel.setPosition(x,y);
		if (plusButton!=null) { 
			plusButton.setPosition(plusButton.getSingleWidget().getX()+deltaX,y);
		}
		if (minusButton!=null) {
			minusButton.setPosition(minusButton.getSingleWidget().getX()+deltaX,y);
		}
		if (editButton!=null) {
			editButton.setPosition(editButton.getSingleWidget().getX()+deltaX,y);
		}
	}
	
	/**
	 * Returns a list of widget components. The list will contain the key/value
	 * label, plus any buttons that have been added.
	 */
	
	@Override
	public List<UIWidget> getWidgets() {
		
		if (editButton!=null) {
			return Arrays.asList(keyValueLabel.getSingleWidget(),editButton.getSingleWidget());
		} else {
			return Arrays.asList(keyValueLabel.getSingleWidget(),plusButton.getSingleWidget(),
					minusButton.getSingleWidget());
		}
	}

	/**
	 * Private class that displays the key and value labels for the enclosing
	 * {@code UIMenuOption}.
	 */
	
	private class UIMenuKeyValueLabel extends UIMenuWidget {
		
		public UIMenuKeyValueLabel() {
			super(LABEL_TEXTURE_WIDTH,LABEL_TEXTURE_HEIGHT,false);
			repaintImages();
		}

		@Override
		protected void paintImage(BufferedImage image) {
			Graphics2D g2=getGraphics(image);
			clearGraphics(image,g2);
			paintBackgroundGradient(g2,LABEL_WIDTH/2,0,LABEL_WIDTH/2,LABEL_HEIGHT);
			g2.setColor(STROKE_COLOR);
			paintBackgroundStroke(g2,LABEL_WIDTH/2,0,LABEL_WIDTH/2,LABEL_HEIGHT);
			g2.setFont(FONT);
			g2.setColor(FONT_COLOR);
			g2.drawString(getLabel(),0,LABEL_HEIGHT-BASELINE);
			g2.drawString(getFormattedValue(),LABEL_WIDTH/2+SPACING,LABEL_HEIGHT-BASELINE);
			g2.dispose();
		}
		
		@Override
		public Dimension getHitArea() {
			return new Dimension(LABEL_WIDTH,LABEL_HEIGHT);
		}
	}
}