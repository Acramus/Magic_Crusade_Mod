package fr.toss.common.player;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import api.player.server.ServerPlayerAPI;
import api.player.server.ServerPlayerBase;
import fr.toss.common.Main;
import fr.toss.common.items.ItemArmorM;
import fr.toss.common.packet.PacketExpToClient;
import fr.toss.common.packet.PacketLogIn;
import fr.toss.common.packet.Packets;
import fr.toss.common.world.TeleporterDim;

public class ServerPlayerBaseMagic extends ServerPlayerBase
{
	public static Map<String, PlayerData> PLAYER_DATA = new HashMap<String, PlayerData>(); //PlayerData is added when the player dies and remove after it respawn
	
	public Item armor[]; // armor[4] = item equipped
	
	public boolean is_poisonned; //voleur

	public int classe;
	public int level;
	public int experience;
	public int experience_to_get;
	public int exp_to_next_level;
	public int endurance;
	
	public ServerPlayerBaseMagic(ServerPlayerAPI playerapi)
	{
		super(playerapi);
		this.armor = new Item[5];
	}

	
	/** Appelez lrosque le joueur mort */

	public void onDeath(net.minecraft.util.DamageSource paramDamageSource)
	{
		super.onDeath(paramDamageSource);
		System.out.println("onDeath: " + this.classe);

		PlayerData data = new PlayerData(this);
		PLAYER_DATA.put(this.player.getCommandSenderName(), data);
	}
	
	/** Appelez lrosque le joueur a respawn */
	public void onRespawn()
	{
		System.out.println("onRespawn: " + this.classe);
		if (PLAYER_DATA.containsKey(this.player.getCommandSenderName()))
		{
			PacketLogIn packet;
			PlayerData data;
			
			data = PLAYER_DATA.get(this.player.getCommandSenderName());
			this.level = data.level;
			this.experience = data.experience;
			this.classe = data.classe;
			this.exp_to_next_level = this.level * 20 * (this.level + 1);
		    this.player.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0d);
			packet = new PacketLogIn(this);
			Packets.network.sendTo(packet, this.getPlayer());
			PLAYER_DATA.remove(this.player.getCommandSenderName());
		}
	}
	
	/** Appelez lorsque le joueur redefinit sa classe */
	public void init(int p_classe)
	{
		this.classe = p_classe;
		this.level = 1;
		this.experience = 0;
		this.exp_to_next_level = this.level * 20 * (this.level + 1);
		this.player.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(0.5d * (this.level - 1) + 20.0d + this.endurance / 10);
	    this.player.setHealth(this.getPlayer().getMaxHealth());
	}
	
	 @Override
	 public void onUpdate()
	 {
		 super.onUpdate();

		 if (this.player.dimension == Main.DIM_ID)
		 {
			 if (this.player.posY < 0)
			 {
				 this.travelToDimension(this.player);
				 this.player.setPosition(this.player.posX, this.player.worldObj.getTopSolidOrLiquidBlock((int) this.player.posX, (int) this.player.posZ), this.player.posZ);
			 }
		 }
		 
		 for (int i = 0; i < 4; i++)
		 {
			 ItemStack is = this.player.inventory.armorInventory[i];
			 if (is != null)
			 {
				 Item item = is.getItem();
				 if (item != this.armor[i])
				 {
					 if (item instanceof ItemArmorM)
					 {
						 this.endurance += ((ItemArmorM)item).endurance;
						 this.player.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(0.5d * (this.level - 1) + 20.0d + this.endurance / 10);
						 if (this.player.getHealth() > this.player.getMaxHealth())
							 this.player.setHealth(this.player.getMaxHealth());
					 }
					 this.armor[i] = item;
				 }	 
			 }
			 else if (is == null && this.armor[i] != null)
			 {
				 if (this.armor[i] instanceof ItemArmorM)
				 {
					 this.endurance -= ((ItemArmorM)this.armor[i]).endurance;
					 this.player.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(0.5d * (this.level - 1) + 20.0d + this.endurance / 10);
					 if (this.player.getHealth() > this.player.getMaxHealth())
						 this.player.setHealth(this.player.getMaxHealth());
				 }
				 this.armor[i] = null;
			 }
		 }
		 
		 if (this.experience_to_get > 0)
		 {
			 this.experience += 8;
			 if (this.experience >= this.exp_to_next_level) //onlevelup
			 {
				 this.experience = 0;
				 this.level += 1;
					this.exp_to_next_level = this.level * 20 * (this.level + 1);
				 this.player.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.player.getMaxHealth() + 0.5d);
				 this.player.setHealth(this.player.getMaxHealth());
			 }
			 this.experience_to_get -= 8;
		 }
	 }

	 @Override
	public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound nbt)
	{
		 super.readEntityFromNBT(nbt);
		 this.classe = nbt.getInteger("classe");
		 this.level = nbt.getInteger("level"); 
		 this.experience = nbt.getInteger("experience"); 
	}
		
	 @Override
	 public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound nbt)
	 {
		 super.writeEntityToNBT(nbt);
		 nbt.setInteger("classe", this.classe); 
		 nbt.setInteger("level", this.level); 
		 nbt.setInteger("experience", this.experience); 
	 }
		
	 @Override
	 public void onKillEntity(net.minecraft.entity.EntityLivingBase entity)
	 {
		 super.onKillEntity(entity);
		 PacketExpToClient packet;
		 
		 this.experience_to_get += entity.getMaxHealth() * 6;

		 packet = new PacketExpToClient(this.experience_to_get);
		 Packets.network.sendTo(packet, this.player);
	 }
	
	 /**
	 * Teleports the entity to another dimension. Params: Dimension number to teleport to
	 */
	    public void travelToDimension(EntityPlayer player)
	    {	
	    	if ((player.ridingEntity == null) && (player.riddenByEntity == null) && ((player instanceof EntityPlayerMP)))
	    	{	    	
	    		EntityPlayerMP thePlayer = (EntityPlayerMP)player;
		    	if (thePlayer.timeUntilPortal > 0)
		    	{
			    	thePlayer.timeUntilPortal = 10;
		    	}
		    	else if (thePlayer.dimension != Main.DIM_ID)
		    	{
			    	thePlayer.timeUntilPortal = 10;
			    	thePlayer.mcServer.getConfigurationManager().transferPlayerToDimension(thePlayer, Main.DIM_ID, new TeleporterDim(thePlayer.mcServer.worldServerForDimension(Main.DIM_ID)));
		    	}
		    	else 
		    	{
			    	thePlayer.timeUntilPortal = 10;
			    	thePlayer.mcServer.getConfigurationManager().transferPlayerToDimension(thePlayer, 0, new TeleporterDim(thePlayer.mcServer.worldServerForDimension(0)));
		    	}
	    	}
	    }
	
	    /** Retournes le joueur */
		public EntityPlayerMP getPlayer() 
		{
			return this.player;
		}
	
		/** Retournes la classe du joueur */
		public int getClasse()
		{
			return this.classe;
		}

		/** Retournes le niveau de classe du joeuur */
		public int getLevel() 
		{
			return this.level;
		}

		/** Retournes le niveau de classe du joeuur */
		public int getExperience()
		{
			return this.experience;
		}
	}