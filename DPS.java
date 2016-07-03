import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DPS extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	static enum CharClass { MONK, ROGUE, WARRIOR;
		static CharClass getCharClass(int i){
			if(i == 0)
				return MONK;
			if(i == 1)
				return ROGUE;
			
			return WARRIOR;
		}
	}
	
	enum WeaponType{ _1HBLUNT, _1HSLASH, PIERCING, _2HBLUNT, _2HSLASH, FIST;
		static WeaponType getWeaponType(int index)
		{
			if(index == 0) return _1HBLUNT;
			if(index == 1) return _1HSLASH;
			if(index == 2) return PIERCING;
			if(index == 3) return _2HBLUNT;
			if(index == 4) return _2HSLASH;
			if(index == 5) return FIST;
			return FIST;
		}
	}
	
	enum Ability{OFFENSE, DOUBLE_ATTACK, DUAL_WIELD, BACKSTAB};
	static final String[] playerClass = {"Monk", "Rogue", "Warrior"};
	static final double MAX_MH_PPM = 2;
	static final double MIN_MH_PPM = .5;
	static final double MAX_OH_PPM = 1;
	static final double MIN_OH_PPM = .25;
	static final int MAX_DEX = 255;
	private JPanel mainPanel = new JPanel();
	private CharPanel charPanel = new CharPanel();
	private WeaponStats weapon_1 = new WeaponStats(WeaponStats.WeaponHand.mhWeapon);
	private WeaponStats weapon_2 = new WeaponStats(WeaponStats.WeaponHand.ohWeapon);
	private JButton calcButton = new JButton();
	private ResultPanel resultPanel = new ResultPanel();
	private boolean isTwoHand = false;
	
	public static void main(String args[])
	{
		DPS app = new DPS();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setTitle("Melee DPS Calculator");
		app.setResizable(false);
		app.pack();
		app.setLocationRelativeTo(null);
		app.setVisible(true);
	}
	
	public DPS()
	{
		calcButton.add(new JLabel("Calculate"));
		calcButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		calcButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				calculateValues();				
			}
		});

		weapon_1.getMainWeapon().addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				toggle2H();
			}
		});
		
		charPanel.getClassMenu().addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				resultPanel.setPanelLayout(CharClass.getCharClass(charPanel.getClassIndex()));
				pack();
			}
		});
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		JPanel inputPanel = new JPanel(new BorderLayout());
		JPanel weaponPanel = new JPanel();
		
		weaponPanel.add(weapon_1);
		weaponPanel.add(weapon_2);
		inputPanel.add(charPanel, BorderLayout.NORTH);
		inputPanel.add(weaponPanel, BorderLayout.CENTER);
		
		mainPanel.add(inputPanel);
		mainPanel.add(calcButton);
		mainPanel.add(resultPanel);
		add(mainPanel);
		setLocation(100,100);
	}
	
	public void calculateValues()
	{
		resultPanel.clearDPS();
		try{
			CharClass class_ = DPS.CharClass.getCharClass(charPanel.getClassIndex());
			int level = charPanel.getLevel();
			
		// Main hand damage calculations
			double weapon_1_dps = 0;
			int max_damage;
			int dmg_cap = damageCap();
			double avg_ppm = 0;
			
			if(getWeaponSkill(
					class_, level,	WeaponType.getWeaponType(weapon_1.getType())
				) == 0)
			{
				weapon_1_dps = 0;
			}
			else
			{
				max_damage = (int)(damageMod() * weapon_1.getDamage()) + damageBonus();
							
				if(max_damage > dmg_cap)
					max_damage = dmg_cap;
				
				weapon_1_dps = max_damage / delay(weapon_1);
				if(getOffensiveSkill(
						class_, level, Ability.DOUBLE_ATTACK
					) != 0)
				{
					weapon_1_dps += (weapon_1_dps * ((charPanel.getLevel() 
							+ getOffensiveSkill(
									class_, level, Ability.DOUBLE_ATTACK)
								) / 500.0));
				}
			}
			
			if(weapon_1.procDamage() != 0)
			{
				avg_ppm = (((MAX_MH_PPM - MIN_MH_PPM) / MAX_DEX) * charPanel.getDex()) + MIN_MH_PPM;
				weapon_1_dps += (avg_ppm * weapon_1.procDamage()) / 60;
			}
			
		// Off-hand damage calculations
			double weapon_2_dps = 0;
			if(isTwoHand  
					|| (weapon_2.getDamage() == 0 && weapon_2.getDelay() == 0) 
					|| getWeaponSkill(
							class_, level, WeaponType.getWeaponType(weapon_2.getType())
					) == 0)
			{
				weapon_2_dps = 0;
			}
			else
			{
				max_damage = (int)(damageMod() * weapon_2.getDamage());
				if(max_damage > dmg_cap)
					max_damage = dmg_cap;
				
				if(getOffensiveSkill(class_, level, Ability.DUAL_WIELD) != 0)
				{
					weapon_2_dps = (max_damage / delay(weapon_2))
						* ((level + getOffensiveSkill(
										class_, level, Ability.DUAL_WIELD
								)) / 400.0);
				}
				
				if(getOffensiveSkill(class_, level, Ability.DOUBLE_ATTACK) >= 150)
				{
					weapon_2_dps += (weapon_2_dps 
						* ((level + getOffensiveSkill(
										class_, level, Ability.DOUBLE_ATTACK
								)) / 500.0));
				}
			}
			
			if(weapon_2.procDamage() != 0)
			{
				avg_ppm = (((MAX_OH_PPM - MIN_OH_PPM) / MAX_DEX) * charPanel.getDex()) + MIN_OH_PPM;
				weapon_2_dps += (avg_ppm * weapon_2.procDamage()) / 60;
			}
			
		// Weapon-based ability DPS
			double specialDPS = calculateSpecialDPS(class_, level);
			
		// Display DPS results
			if(specialDPS > 0)
				resultPanel.setSpecialDPS(specialDPS);
			
			resultPanel.setDPS1(weapon_1_dps);
			resultPanel.setDPS2(weapon_2_dps);
			resultPanel.totalDPS();
		}
		catch(NumberFormatException e){
			JOptionPane.showMessageDialog(null, "All values must be integers.", 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(Exception e){
			JOptionPane.showMessageDialog(null, e.toString(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public double calculateSpecialDPS(CharClass class_, int level_)
	{
		double dps = 0;
		switch(class_)
		{
			case ROGUE :
			{
				if(WeaponType.getWeaponType(weapon_1.getType()) == WeaponType.PIERCING)
				{
					int str = charPanel.getStrength();
					int excessStr = 0;
					int max_hit;
					int min_hit;
					double mult;
					
					if(str > 200)
					{
						excessStr = str - 200;
						str = 200;
					}
					
					mult = 2 + (getOffensiveSkill(class_, level_, Ability.BACKSTAB)* .02);
					
					max_hit = (int)((getOffensiveSkill(class_, level_, Ability.OFFENSE)
						+ str + (excessStr / 5)) * weapon_1.getDamage() * mult) / 100;
					
					if(level_ < 51)
						min_hit = (level_ * 15 / 10);
					else
						min_hit = (level_ * (level_ * 5 - 105)) / 100;
						
					dps = ((max_hit + min_hit) / 2) / (8 / (1 + charPanel.getHaste()) + .5);
				}
				else
					dps = 0;
			}
		}
		return dps;
	}
		
	public double damageMod()
	{
		double mod = (getWeaponSkill(
				DPS.CharClass.getCharClass(
						charPanel.getClassIndex()), 
						charPanel.getLevel(), 
						WeaponType.getWeaponType(weapon_1.getType())
				) + charPanel.getStrength() - 75) / 100.0;
		if(mod < 2)
			mod = 2;
		
		return mod;
	}
	
	public int damageCap()
	{
		int level = charPanel.getLevel();
		if(level < 10)
		{
			return 20;
		}
		else if(level < 20)
		{
			return 28;
		}
		else if(level < 30)
		{
			return 60;
		}
		else
		{
			return Integer.MAX_VALUE;
		}
	}
	
	public int damageBonus()
	{
		if(charPanel.getLevel() < 28)
			return 0;
		
		if(!isTwoHand)
			return (charPanel.getLevel() - 25) / 3;
		
		int delay = weapon_1.getDelay();
		if(delay <= 27)
			return (charPanel.getLevel() - 22) / 3;
		
		int base;
		if(charPanel.getLevel() > 50)
			base = (charPanel.getLevel() - 7) / 3;
		else
			base = (charPanel.getLevel() - 25) / 2;
		
		if(delay <= 39)
			return base;
		else if (delay <= 42)
			return base + 1;
		else if (delay <= 44)
			return base + 3;
		else
			return base + (delay - 31) / 3;
	}
	
	public double delay(WeaponStats weap) throws Exception
	{
		if(weap.getDelay() == 0)
			throw new Exception("Delay cannot be zero.");
		
		int delay = (int)(weap.getDelay() / (1 + charPanel.getHaste()) + .5);
		if(delay < 5)
			delay = 5;
		
		return delay / 10.0;
	}

	public void toggle2H()
	{
		if(weapon_1.getMainWeapon().getSelectedIndex() == 3 ||
				 weapon_1.getMainWeapon().getSelectedIndex() == 4)
		{
			weapon_2.disable();
			isTwoHand = true;
		}
		else
		{
			weapon_2.enable();
			isTwoHand = false;
		}
	}
	
	public int getOffensiveSkill(DPS.CharClass class_, int level, Ability ability)
	{
		int skill = 0;
		switch(ability)
		{
			case OFFENSE :
			{
				switch(class_)
				{
					case ROGUE :
					case WARRIOR :
					{
						skill = 5 + level * 5;
						if(level < 51 && skill > 210)
								skill = 210;
						
						if(skill > 252)
							skill = 252;
						
						break;
					}
					case MONK :
					{
						skill = 5 + level * 5;
						if(level < 51 && skill > 230)
							skill = 230;
						
						if(skill > 252)
							skill = 252;
						break;
					}
				}
				break;
			}
			case DOUBLE_ATTACK :
			{
				switch(class_)
				{
					case MONK :
					{
						skill = 5 + level * 5;
						if(level < 15)
							skill = 0;
						
						if(level < 51 && skill > 210)
							skill = 210;
						
						if(skill > 250)
							skill = 250;
						
						break;						
					}
					case ROGUE :
					{
						skill = 5 + level * 5;
						if(level < 16)
							skill = 0;
						
						if(level < 51 && skill > 200)
								skill = 200;
						
						if(skill > 240)
							skill = 240;
						
						break;
					}
					case WARRIOR :
					{
						skill = 5 + level * 5;
						if(level < 15)
							skill = 0;
						if(level < 51 && skill > 200)
							skill = 200;
						if(skill > 245)
							skill = 245;
						break;
					}
					default : 
					{
						break;
					}
				}
				break;
			}
			case DUAL_WIELD :
			{
				switch(class_)
				{
					case MONK :
					{
						skill = (level + 1) * 7;
						if(skill > 252)
							skill = 252;
						
						break;
					}
					case ROGUE :
					case WARRIOR :
					{
						skill = (level + 1) * 6;
						if(level < 13)
							skill = 0;
						
						if(level < 51 && skill > 210)
								skill = 210;
						
						if(skill > 245)
							skill = 245;
						
						break;
					}
					default : 
					{
						break;
					}
				}
				break;
			}
			case BACKSTAB :
			{
				switch(class_)
				{
					case ROGUE :
					{
						skill = 5 + level * 5;
						if(level < 10)
							skill = 0;
						
						if(level < 51 && skill > 200)
							skill = 200;
						
						if(skill > 225)
							skill = 225;
						
						break;
					}
					default :
					{
						break;
					}
				}
				break;
			}
			default :
			{
				break;
			}
		}
		
		return skill;
	}
	
	public int getWeaponSkill(DPS.CharClass class_, int level, WeaponType weaponType)
	{
		int skill = 0;
		switch(class_)
		{
			case MONK : 
			{
				skill = 5 + level * 5;
				if(level < 51 && skill > 240)
					skill = 240;
				
				switch(weaponType)
				{
					case FIST:
					{
						if(skill > 255 & level < 51)
							skill = 255;
						break;
					}
					case PIERCING:
					case _1HSLASH:
					case _2HSLASH:
					{
						skill = 0;
						break;
					}
					default: break;
				}
				break;
			}
			case ROGUE :
			{
				skill = 5 + level * 5;
				if(level > 50 && skill > 250)
					skill = 250;
				
				if(level < 51)
				{
					if(skill > 200 && weaponType != WeaponType.PIERCING)
						skill = 200;
					
					if(skill > 210 && weaponType == WeaponType.PIERCING)
						skill = 210;
				}
				switch(weaponType)
				{
					case FIST:
					{
						skill = 100;
						break;
					}
					case _2HBLUNT:
					case _2HSLASH:
					{
						skill = 0;
						break;
					}
					default : break;
				}
				break;
			}
			case WARRIOR :
			{
				skill = 5 + level * 5;
				if(level < 51 && skill > 200)
					skill = 200;
				if(level > 50 && skill > 250)
					skill = 250;
				
				switch(weaponType)
				{
					case PIERCING :
					{
						if(skill > 240)
							skill = 240;
						break;
					}
					case FIST :
					{
						if(skill > 100)
							skill = 100;
						break;
					}
					default : break;
				}
				break;
			}
			default : break;
		}
		return skill;
	}
}

class CharPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private InputField level = new InputField("Char Level:");
	private InputField strength = new InputField("Strength:");
	private InputField dex = new InputField("Dexterity:");
	private InputField haste = new InputField("Haste (%):");
	private JComboBox<String> charClass = new JComboBox<String>(DPS.playerClass); 
	
	public CharPanel()
	{
		JPanel rightPanel = new JPanel();
		JPanel leftPanel = new JPanel(new GridLayout(4,1,0,0));
		JPanel classSelectPanel = new JPanel(new GridLayout(3,1,0,0));
		setLayout(new GridLayout(1,2));
				
		leftPanel.add(level);
		leftPanel.add(strength);
		leftPanel.add(dex);
		leftPanel.add(charClass);
		leftPanel.add(haste);
		
		classSelectPanel.add(new JLabel());
		classSelectPanel.add(new JLabel("Char Class:"));
		classSelectPanel.add(charClass);
		classSelectPanel.setAlignmentY(90);
		rightPanel.add(Box.createRigidArea(new Dimension(0,30)));
		rightPanel.add(classSelectPanel);
		
		
		add(leftPanel);
		add(rightPanel);
	}
	
	public int getLevel()
	{
		return level.getValue();
	}
	
	public int getStrength()
	{
		return strength.getValue();
	}
	
	public int getDex()
	{
		return dex.getValue();
	}
	
	public double getHaste()
	{
		return haste.getValue() / 100.0; 
	}
	
	public int getClassIndex()
	{
		return charClass.getSelectedIndex();
	}
	
	public JComboBox<String> getClassMenu()
	{
		return charClass;
	}
}

class InputField extends JPanel
{
	private static final long serialVersionUID = 1L;
	private JTextField value = new JTextField(3);
	
	public InputField(String label)
	{
		value.addFocusListener(new FocusAdapter(){
			public void focusGained(FocusEvent f){
				value.selectAll();
			}
		});
			
		setLayout(new FlowLayout(FlowLayout.RIGHT));
		setValue(0);
		add(new JLabel(label + "  "));
		add(Box.createHorizontalGlue());
		add(value);
	}
	
	public int getValue()
	{
		if(value.getText().contentEquals(""))
			return 0;
		
		return Integer.parseInt(value.getText());
	}
	
	public void enable()
	{
		value.setEditable(true);
	}
	
	public void disable()
	{
		value.setEditable(false);
	}
	
	public void setValue(int i)
	{
		value.setText(String.valueOf(i));
	}
}

class WeaponStats extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private InputField damage;
	private InputField delay;
	private InputField procDamage;
	private JComboBox<String> weaponList;
	
	enum WeaponType{ _1HBLUNT, _1HSLASH, PIERCING, _2HBLUNT, _2HSLASH, FIST;
		static WeaponType getWeaponType(int index)
		{
			if(index == 0) return _1HBLUNT;
			if(index == 1) return _1HSLASH;
			if(index == 2) return PIERCING;
			if(index == 3) return _2HBLUNT;
			if(index == 4) return _2HSLASH;
			if(index == 5) return FIST;
			return FIST;
		}
	}
	
	enum WeaponHand{
		mhWeapon,
		ohWeapon;
		
		static String[] mhWeapons(){
			return (new String[] {"1H Blunt", "1H Slash", "Piercing", "2H Blunt", "2H Slash", "Fist"} );
		}
		
		static String[] ohWeapons(){
			return (new String[]{"1H Blunt", "1H Slash", "Piercing", "Fist"});
		}
	}
	
	public WeaponStats(WeaponHand weaponType)
	{
		JPanel comboPanel = new JPanel();
		JLabel weaponLabel = new JLabel("Weapon ");
		switch(weaponType)
		{
			case mhWeapon : {
				weaponList = new JComboBox<String>(WeaponHand.mhWeapons());
				comboPanel.add(weaponList);
				weaponLabel.setText(weaponLabel.getText() + "1");
			}
				break;
			case ohWeapon : {
				weaponList = new JComboBox<String>(WeaponHand.ohWeapons());
				comboPanel.add(weaponList);
				weaponLabel.setText(weaponLabel.getText() + "2");
			}
		}
		
		weaponLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		damage = new InputField("Damage:");
		delay = new InputField("Delay:");
		procDamage = new InputField("Proc Dmg:");
		procDamage.setValue(0);
				
		add(weaponLabel);
		add(damage);
		add(delay);
		add(procDamage);
		add(comboPanel);
		setBorder(new javax.swing.border.EtchedBorder());
		
		if(weaponLabel.getText().contentEquals("Weapon 2"));
		{
			damage.setValue(0);
			delay.setValue(0);
		}
	}
	
	public JComboBox<String> getMainWeapon()
	{
		return weaponList;
	}
	
	public void disable()
	{
		damage.setEnabled(false);
		delay.setEnabled(false);
		weaponList.setEnabled(false);
	}
	
	public void enable()
	{
		damage.setEnabled(true);
		delay.setEnabled(true);
		weaponList.setEnabled(true);
	}
	
	public int getDamage()
	{
		return damage.getValue();
	}
	
	public int getDelay()
	{
		return delay.getValue();
	}
	
	public int getType()
	{
		return weaponList.getSelectedIndex();
	}
	
	public int procDamage()
	{
		return procDamage.getValue();
	}
}

class ResultPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private JLabel w1dps = new JLabel("0.0");
	private JLabel w2dps = new JLabel("0.0");
	private JLabel specialdps = new JLabel("0.0");
	private JLabel totaldps = new JLabel("0.0");
	private JPanel resultsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
	private JPanel labelPanel = new JPanel(new GridLayout(3, 1, 5, 5));
	
	public ResultPanel()
	{
		setLayout(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		labelPanel.add(new JLabel("Weapon 1 DPS:  "));
		labelPanel.add(new JLabel("Weapon 2 DPS:  "));
		labelPanel.add(new JLabel("Total DPS:"));
		
		resultsPanel.add(w1dps);
		resultsPanel.add(w2dps);
		resultsPanel.add(totaldps);
		
		add(labelPanel, BorderLayout.WEST);
		add(resultsPanel, BorderLayout.CENTER);
	}
	
	public void setDPS1(double newDPS)
	{
		newDPS = Math.floor(newDPS * 1000) / 1000;
		w1dps.setText(String.valueOf(newDPS));
	}
	
	public void setDPS2(double newDPS)
	{
		newDPS = Math.floor(newDPS * 1000) / 1000;
		w2dps.setText(String.valueOf(newDPS));
	}
	
	public void setSpecialDPS(double newDPS)
	{
		newDPS = Math.floor(newDPS * 1000) / 1000;
		specialdps.setText(String.valueOf(newDPS));
	}
	
	public void totalDPS()
	{
		double dps = 0;
		for(int i = 0; i < resultsPanel.getComponentCount(); i++)
			dps += Double.parseDouble(((JLabel)resultsPanel.getComponent(i)).getText());
		
		totaldps.setText(String.valueOf((Math.floor(dps * 1000) / 1000)));
	}
	
	public void clearDPS()
	{
		for(int i = 0; i < resultsPanel.getComponentCount(); i++)
			((JLabel)resultsPanel.getComponent(i)).setText("0.0");
	}
	
	public void setPanelLayout(DPS.CharClass class_)
	{
		switch(class_)
		{
			case ROGUE : 
			{
				while(labelPanel.getComponentCount() > 2)
					labelPanel.remove(2);
				
				labelPanel.setLayout(new GridLayout(4,1,5,5));
				labelPanel.add(new JLabel("Backstab DPS:"));
				labelPanel.add(new JLabel("Total DPS:"));
				
				
				while(resultsPanel.getComponentCount() > 2)
					resultsPanel.remove(2);
				
				resultsPanel.setLayout(new GridLayout(4,1,5,5));
				resultsPanel.add(specialdps);
				resultsPanel.add(totaldps);
				add(resultsPanel, BorderLayout.CENTER);
				break;
			}
			default :
			{
				while(labelPanel.getComponentCount() > 2)
					labelPanel.remove(2);
				
				labelPanel.setLayout(new GridLayout(3,1,5,5));
				labelPanel.add(new JLabel("Total DPS"));
				
				while(resultsPanel.getComponentCount() > 2)
					resultsPanel.remove(2);	
				
				resultsPanel.setLayout(new GridLayout(3,1,5,5));
				resultsPanel.add(totaldps);
			}
		}
	}
}