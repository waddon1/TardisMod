package tardis.common.tileents;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;

import io.darkcraft.darkcore.mod.abstracts.AbstractTileEntity;
import io.darkcraft.darkcore.mod.datastore.SimpleCoordStore;
import io.darkcraft.darkcore.mod.helpers.MathHelper;
import io.darkcraft.darkcore.mod.helpers.ServerHelper;
import io.darkcraft.darkcore.mod.helpers.WorldHelper;
import io.darkcraft.darkcore.mod.interfaces.IExplodable;

import tardis.TardisMod;
import tardis.api.IControlMatrix;
import tardis.api.ScrewdriverMode;
import tardis.api.TardisFunction;
import tardis.api.TardisPermission;
import tardis.api.TardisUpgradeMode;
import tardis.common.TMRegistry;
import tardis.common.core.HitPosition;
import tardis.common.core.TardisOutput;
import tardis.common.core.flight.FlightConfiguration;
import tardis.common.core.helpers.Helper;
import tardis.common.core.helpers.ScrewdriverHelper;
import tardis.common.core.helpers.ScrewdriverHelperFactory;
import tardis.common.dimension.TardisDataStore;
import tardis.common.dimension.damage.ExplosionDamageHelper;
import tardis.common.dimension.damage.TardisDamageSystem;
import tardis.common.tileents.extensions.upgrades.AbstractUpgrade;
import tardis.common.tileents.extensions.upgrades.factory.UpgradeFactory;

public class EngineTileEntity extends AbstractTileEntity implements IControlMatrix, IExplodable
{
	private String[]			currentUsers;
	private int					currentUserID;
	public String				currentPerson;

	public int					lastButton				= -1;
	public int					lastButtonTT			= -1;
	private TardisUpgradeMode	preparingToUpgrade		= null;
	private int					preparingToUpgradeTT	= -1;

	private boolean				internalOnly			= false;

	private ScrewdriverHelper	screwHelper;

	private int					consoleSettingControl	= 0;
	private int					protectedRadius			= 0;
	private static final int	maxProtectedRadius		= 16 * 10;
	private String				consoleSettingString	= "Main";	// The string displayed on the console room selection screen.
	private static String[]		availableConsoleRooms	= null;

	public boolean				isEngineOpen			= false;
	public double				visibility				= 1;


	public static void updateConsoleRooms()
	{
		availableConsoleRooms = TardisMod.schemaHandler.getSchemas(true);
	}

	private boolean importantButton(int button)
	{
		switch(button)
		{
			case 10:
			case 11:
			case 12:
			case 13:
			case 73: return true;
			default: return false;
		}
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		if(ServerHelper.isClient())
		{
			if(!isEngineOpen && (visibility < 1))
				visibility += 0.1;
			else if(isEngineOpen && (visibility > 0))
				visibility -= 0.1;
		}
		if (((tt % 40) == 1) && ServerHelper.isServer())
		{
			if (availableConsoleRooms == null)
				refreshAvailableConsoleRooms();
			verifyEngineBlocks();
			getUsernames();
		}

		if ((lastButtonTT != -1) && (tt > (lastButtonTT + (importantButton(lastButton) ? FlightConfiguration.shiftPressTime : 20))))
		{
			lastButton = -1;
			lastButtonTT = -1;
		}

		if ((preparingToUpgrade != null) && (tt > (preparingToUpgradeTT + 80)))
		{
			preparingToUpgrade = null;
			preparingToUpgradeTT = -1;
			if (ServerHelper.isServer())
				sendUpdate();
		}
	}

	private void getUsernames()
	{
		currentUsers = MinecraftServer.getServer().getAllUsernames();
		Arrays.sort(currentUsers);
		setUsername();
	}

	private void setUsername()
	{
		if ((currentUsers != null) && (currentUsers.length > 0))
		{
			currentUserID = MathHelper.cycle(currentUserID, 0, currentUsers.length - 1);
			currentPerson = currentUsers[currentUserID];
		}
		else
			currentPerson = "";
	}

	private void verifyEngineBlocks()
	{
		if (worldObj.getBlock(xCoord, yCoord - 1, zCoord) == Blocks.air)
			worldObj.setBlock(xCoord, yCoord - 1, zCoord, TMRegistry.schemaComponentBlock, 7, 3);
		if (worldObj.getBlock(xCoord, yCoord + 1, zCoord) == Blocks.air)
			worldObj.setBlock(xCoord, yCoord + 1, zCoord, TMRegistry.schemaComponentBlock, 7, 3);
		if (worldObj.getBlock(xCoord, yCoord + 2, zCoord) == Blocks.air)
			worldObj.setBlock(xCoord, yCoord + 2, zCoord, TMRegistry.schemaComponentBlock, 7, 3);
	}

	public int getControlFromHit(HitPosition hit)
	{
		TardisDataStore ds = Helper.getDataStore(this);
		if (hit.within(2, 2.318, 0.170, 2.432, 0.830))
			return 0;
		else if (hit.within(2, 0.558, 0.685, 0.660, 0.768))
			return 4;
		else if (hit.within(2, 0.488, 0.685, 0.564, 0.768))
			return 5;
		else if (hit.within(5, 0.393, 0.725, 0.545, 0.876))
			return 10;
		else if (hit.within(5, 0.393, 0.523, 0.545, 0.679))
			return 11;
		else if (hit.within(5, 0.393, 0.326, 0.545, 0.477))
			return 12;
		else if (hit.within(5, 0.393, 0.122, 0.545, 0.276))
			return 13;
		else if (hit.within(5, 0.551, 0.711, 0.704, 0.898))
			return 20;
		else if (hit.within(5, 0.551, 0.510, 0.704, 0.696))
			return 21;
		else if (hit.within(5, 0.551, 0.311, 0.704, 0.492))
			return 22;
		else if (hit.within(5, 0.551, 0.108, 0.704, 0.294))
			return 23;
		else if (hit.within(5, 0.729, 0.711, 0.888, 0.898))
			return 30;
		else if (hit.within(3, 0.42, 0.532, 0.59, 0.664))
			return 39;
		else if (hit.within(3, 0.360, 0.274, 0.440, 0.346))
			return 41;
		else if (hit.within(3, 0.460, 0.274, 0.540, 0.346))
			return 44;
		else if (hit.within(3, 0.560, 0.274, 0.640, 0.346))
			return 45;
		else if (hit.within(3, 0.360, 0.360, 0.440, 0.438))
			return 51;
		else if (hit.within(3, 0.460, 0.360, 0.540, 0.438))
			return 54;
		else if (hit.within(3, 0.560, 0.360, 0.640, 0.438))
			return 55;
		else if (hit.within(3, 0.743, 0.254, 0.876, 0.375))
			return 60;
		else if (hit.within(2, 0.716, 0.036, 0.832, 0.674))
			return 70;
		else if (hit.within(2, 0.783, 0.687, 0.859, 0.769))
			return 71;
		else if (hit.within(2, 0.691, 0.687, 0.768, 0.769))
			return 72;
		else if (hit.within(2, 0.735, 0.782, 0.813, 0.859))
			return 73;
		else if (hit.side == 2)
		{
			for(TardisPermission p : TardisPermission.values())
			{
				int o = p.ordinal();
				double d = 0.078;
				double x = 0.68 - (o * d);
				double X = 0.68 - ((o + 1) * d);
				if(hit.within(2, 0.33, X, 0.43, x))
					return 80 + o;
				if(hit.within(2, 0.43, X, 0.53, x))
					return 90 + o;
			}
		}
		else if(hit.within(4, 0.450, 0.01, 0.525, 0.09))
			return 100;
		else if((ds != null) && ds.hasFunction(TardisFunction.SPAWNPROT) && hit.within(3, 0.675, 0.450, 0.925, 0.540))
			return 130;
		else if(hit.within(3, 0.450, 0.72, 0.55, 0.82))
			return 131;
		else if(hit.within(3, 0.76, 0.61, 0.87, 0.72))
			return 132;
		return -1;
	}

	private int getControlFromEngineHit(HitPosition hit)
	{
		if(hit.side == 4)
		{
			//System.out.println("EHIT!");
			double d = 0.094;
			for(int i = 0; i < 8; i++)
			{
				double x = 0.135 + (d * i);
				double X = x + 0.07;
				if(hit.within(4, 0.950, x, 1.083, X))
					return 100 + i + 1;
			}
			if(hit.within(4, 0.495, 0.400, 0.700, 0.600))
				return 110;
			else if(hit.within(4, 0.75, 0.65, 0.85, 0.75))
				return 111;
			else if(hit.within(4, 0.55, 0.70, 0.65, 0.80))
				return 112;
			else if(hit.within(4, 0.35, 0.64, 0.45, 0.75))
				return 113;
			else if(hit.within(4, 0.75, 0.25, 0.85, 0.35))
				return 114;
			else if(hit.within(4, 0.55, 0.20, 0.66, 0.30))
				return 115;
			else if(hit.within(4, 0.35, 0.25, 0.45, 0.35))
				return 116;
			else if(hit.within(4, 0.15, 0.15, 0.30, 0.30))
				return 117;
			else if(hit.within(4, 0.15, 0.40, 0.32, 0.58))
				return 119;
			else if(hit.within(4, 0.21, 0.61, 0.29, 0.69))
				return 118;
		}
		return -1;
	}

	private double posY(EntityPlayer player)
	{
		if(ServerHelper.isClient()) return player.posY;
		return player.posY;
	}

	public int getControlFromHit(int blockX, int blockY, int blockZ, Vec3 hitPos, EntityPlayer pl)
	{
		int side = hitPos.xCoord == 0 ? 4 : (hitPos.xCoord == 1 ? 5 : (hitPos.zCoord == 0 ? 2 : 3));
		float relativeY = (float) (hitPos.yCoord - yCoord);
		float relativeX = (float) ((side >= 4) ? hitPos.zCoord : hitPos.xCoord);
		HitPosition hit = new HitPosition(relativeX, relativeY, side);
		if(hit.within(4,0.1, 0.1, 1.12, 0.9) && isEngineOpen)
		{
			double extraX = 0.05;
			double xD = -pl.posX;
			double mult = ((xD+extraX)/xD) - 1;
			double yD = (hitPos.yCoord) - posY(pl);
			double yO = relativeY + (yD * mult);
			double zO = relativeX + ((hitPos.zCoord - pl.posZ) * mult);
			hit = new HitPosition((float) zO,(float) yO,side);
			return getControlFromEngineHit(hit);
		}
		return getControlFromHit(hit);
	}

	public boolean activate(EntityPlayer pl, int side, int blockY, float x, float y, float z)
	{
		if (ServerHelper.isServer())
			return true;
		float relativeY = (blockY - yCoord) + y;
		float relativeX = (side >= 4) ? z : x;
		HitPosition hit = new HitPosition(relativeX, relativeY, side);
		int control = -1;
		if(hit.within(4,0.1, 0.1, 1.12, 0.9) && isEngineOpen)
		{
			double extraX = 0.05;
			double xD = -pl.posX;
			double mult = ((xD+extraX)/xD) - 1;
			double yD = (blockY+y) - posY(pl);
			double yO = relativeY + (yD * mult);
			double zO = relativeX + ((z - pl.posZ) * mult);
			hit = new HitPosition((float) zO,(float) yO,side);
			control = getControlFromEngineHit(hit);
		}
		else
			control = getControlFromHit(hit);
		if (control != -1)
			Helper.activateControl(this, pl, control);
		else
			TardisOutput.print("TETE", hit.toString());
		return true;
	}

	private static final String hasPerm = " has permission to ";
	private static final String hasNoPerm = " does not have permission to ";
	@Override
	public void activateControl(EntityPlayer pl, int control)
	{
		if(ServerHelper.isClient()) return;
		int prevLastButton = lastButton;
		lastButton = control;
		lastButtonTT = tt;
		TardisOutput.print("TETE", "Control activated:" + control);
		CoreTileEntity core = Helper.getTardisCore(worldObj);
		TardisDataStore ds = Helper.getDataStore(worldObj);
		if ((core != null) && (ds != null))
		{
			if ((control == 4) || (control == 5))
			{
				currentUserID += control == 4 ? 1 : -1;
				setUsername();
			}
			else if ((control >= 10) && (control < 20))
			{
				if (ds.unspentLevelPoints() > 0)
				{
					if (ds.hasPermission(pl, TardisPermission.POINTS))
					{
						TardisUpgradeMode mode = TardisUpgradeMode.getUpgradeMode(control - 10);
						TardisOutput.print("TETE", "Setting mode to " + mode.name);
						if (mode != null)
						{
							if ((preparingToUpgrade == mode) && pl.isSneaking())
							{
								preparingToUpgrade = null;
								preparingToUpgradeTT = -1;
								ds.upgradeLevel(mode, 1);
								core.sendUpdate();
							}
							else if (preparingToUpgrade == null)
							{
								preparingToUpgrade = mode;
								preparingToUpgradeTT = tt;
								pl.addChatMessage(new ChatComponentText(
										"[ENGINE] Sneak and activate the button again to upgrade " + mode.name));
							}
						}
					}
					else
					{
						pl.addChatMessage(CoreTileEntity.cannotModifyMessage);
					}
				}
			}
			else if ((control >= 20) && (control < 30))
			{
				TardisUpgradeMode mode = TardisUpgradeMode.getUpgradeMode(control - 20);
				if (mode != null)
				{
					int level = ds.getLevel(mode);
					int tlevel = ds.getLevel(mode, true);
					pl.addChatMessage(new ChatComponentText("[ENGINE] " + mode.name + " lvl: " + level + "(" + tlevel + ")/"
							+ ds.maxUnspentLevelPoints()));
				}
			}
			else if (control == 30)
				pl.addChatMessage(new ChatComponentText("[ENGINE] Unspent level points: " + ds.unspentLevelPoints() + "/"
						+ ds.maxUnspentLevelPoints()));
			else if (control == 39)
			{
				if (screwHelper != null)
				{
					ItemStack toGive = screwHelper.getItemStack();
					TMRegistry.screwItem.notifyMode(screwHelper, pl, false);
					screwHelper = null;
					WorldHelper.giveItemStack(pl, toGive);
				}
				else
				{
					ScrewdriverHelper helper = ScrewdriverHelperFactory.get(pl.getHeldItem());
					if (helper != null)
					{
						screwHelper = helper;
						helper.setOwner(core.getOwner());
						helper.clear();
						InventoryPlayer inv = pl.inventory;
						inv.mainInventory[inv.currentItem] = null;
					}
				}
			}
			else if ((control >= 40) && (control < 50))
			{
				if (screwHelper != null)
				{
					if (ds.hasPermission(pl, TardisPermission.PERMISSIONS))
						screwHelper.togglePermission(ScrewdriverMode.get(control - 40));
					else
						ServerHelper.sendString(pl, CoreTileEntity.cannotModifyMessage);
				}

			}
			else if ((control >= 50) && (control < 60))
			{
				if (screwHelper != null)
				{
					ScrewdriverMode m = ScrewdriverMode.get(control - 50);
					String modeString = m.name();
					String s = "Sonic screwdriver ";
					if ((m.requiredFunction == null) || core.hasFunction(m.requiredFunction))
					{
						s += screwHelper.hasPermission(m) ? "has" : "does not have";
						s += " " + modeString + " permission";
					}
					else
					{
						s += "does not have " + modeString + " functionality";
					}
					ServerHelper.sendString(pl, "ENGINE", s);
				}
			}
			else if (control == 60)
				internalOnly = !internalOnly;
			else if ((control == 71) || (control == 72))
			{
				if ((availableConsoleRooms == null) || (availableConsoleRooms.length == 1))
					updateConsoleRooms();
				consoleSettingControl = MathHelper.cycle(consoleSettingControl + (control == 71 ? -1 : 1), 0,
						availableConsoleRooms.length - 1);
				consoleSettingString = availableConsoleRooms[consoleSettingControl];
			}
			else if (control == 73)
			{
				if (ds.hasPermission(pl, TardisPermission.ROOMS))
				{
					if ((prevLastButton != 73) && pl.isSneaking())
						lastButton = -1;
					else if ((prevLastButton == 73) && !pl.isSneaking())
						lastButton = -1;
					else if (prevLastButton != 73)
					{
						ServerHelper
								.sendString(pl, "ENGINE",
										"Warning: Changing console rooms may replace blocks. Please right click, then sneak-right click this button to proceed");
					}
					else
					{
						core.loadConsoleRoom(sanitiseConsole(consoleSettingString));
					}
				}
				else
				{
					lastButton = -1;
					ServerHelper.sendString(pl, "ENGINE", CoreTileEntity.cannotModifyMessage.getFormattedText());
				}
			}
			else if((control >= 80) && (control < 90))
			{
				TardisPermission p = TardisPermission.get(control - 80);
				if(ds.togglePermission(pl, currentPerson, p))
					core.sendUpdate();
				else
					ServerHelper.sendString(pl, CoreTileEntity.cannotModifyPermissions);
			}
			else if((control >= 90) && (control < 100))
			{
				TardisPermission p = TardisPermission.get(control - 90);
				ServerHelper.sendString(pl, currentPerson + (ds.hasPermission(currentPerson, p) ? hasPerm : hasNoPerm) + p.name);
			}
			else if(control == 100)
			{
				if(ds.hasPermission(pl, TardisPermission.POINTS))
					isEngineOpen = !isEngineOpen;
			}
			else if((control == 130) && ds.hasFunction(TardisFunction.SPAWNPROT))
			{
				int dir = pl.isSneaking() ? -16 : 16;
				protectedRadius += dir;
				protectedRadius = MathHelper.clamp(protectedRadius, 0, maxProtectedRadius);
			}
			else if((control == 131) && (screwHelper != null))
			{
				screwHelper.cycleScrewdriverType();
			}
			else if(control == 132)
			{
				ds.setSpaceProjection(!ds.getSpaceProjection());
			}
			else if(isEngineOpen)
			{
				if((control >= 101) && (control <= 108))
				{
					if(ds.hasPermission(pl, TardisPermission.POINTS))
					{
						int slot = control - 101;
						if(!addUpgrade(slot, pl, ds))
							removeUpgrade(slot, pl, ds);
						core.sendUpdate();
					}
					else
						ServerHelper.sendString(pl, CoreTileEntity.cannotModifyMessage);
				}
				if((control >= 110) && (control <= 119))
				{
					int component = control - 110;
					ds.damage.repairComponent(pl, component);
				}
			}
		}
	}

	private boolean addUpgrade(int slot, EntityPlayer pl, TardisDataStore ds)
	{
		ItemStack is = pl.getHeldItem();
		if(is != null)
		{
			AbstractUpgrade up = UpgradeFactory.createUpgrade(is);
			if(up != null)
			{
				if(ds.upgrades[slot] == null)
				{
					if(up.isValid(ds.upgrades))
					{
						ds.upgrades[slot] = up;
						up.setEnginePos(coords);
						if (!pl.capabilities.isCreativeMode)
						{
							pl.inventory.decrStackSize(pl.inventory.currentItem, 1);
							pl.inventory.markDirty();
						}
						System.out.println("Added upgrade:" + up);
						ds.markDirty();
						return true;
					}
				}
			}
		}
		return false;
	}

	private void removeUpgrade(int slot, EntityPlayer pl, TardisDataStore ds)
	{
		AbstractUpgrade up = ds.upgrades[slot];
		if(up != null)
		{
			ds.upgrades[slot] = null;
			WorldHelper.giveItemStack(pl, up.getIS());
			ds.markDirty();
		}
	}

	@Override
	public double getControlState(int controlID, boolean wobble)
	{
		double maxWobble = 0.025;
		double count = 20;
		int maxRand = 10;
		double wobbleAmount = 0;
		if (wobble)
		{
			wobbleAmount = (((tt + rand.nextInt(maxRand)) % count) / count);
			wobbleAmount = Math.abs(wobbleAmount - 0.5) * maxWobble * 2;
		}
		return getControlState(controlID) + wobbleAmount;
	}

	@Override
	public double getControlState(int cID)
	{
		if (ServerHelper.isServer())
			return 0;
		CoreTileEntity core = Helper.getTardisCore(worldObj);
		TardisDataStore ds = Helper.getDataStore(worldObj);
		if ((core != null) && (ds != null))
		{
			if ((cID == 4) || (cID == 5) || ((cID >= 10) && (cID < 20)) || ((cID >= 71) && (cID <= 73)) || (cID == 131))
				return (lastButton == cID) ? 1.0 : 0;
			if ((cID >= 20) && (cID < 30))
			{
				TardisUpgradeMode mode = TardisUpgradeMode.getUpgradeMode(cID - 20);
				if ((mode != null) && (ds.maxUnspentLevelPoints() > 0))
					return ((double) ds.getLevel(mode,true)) / ((double) ds.maxUnspentLevelPoints());
				return 0;
			}
			if (cID == 30)
			{
				if (ds.maxUnspentLevelPoints() > 0)
					return ((double) ds.unspentLevelPoints()) / ((double) ds.maxUnspentLevelPoints());
				return 0;
			}
			if ((cID >= 40) && (cID < 60))
			{
				if (screwHelper == null)
					return 0;
				int mID = cID >= 50 ? cID - 50 : cID - 40;
				ScrewdriverMode m = ScrewdriverMode.get(mID);
				if (cID < 50)
					return screwHelper.hasPermission(m) ? 1 : 0;
				else
				{
					if ((m.requiredFunction == null) || core.hasFunction(m.requiredFunction))
					{
						double v = screwHelper.hasPermission(m) ? 1 : 0.2;
						return v;
					}
					return 0.2;
				}
			}
			if (cID == 60)
				return internalOnly ? 1 : 0;
			if ((cID >= 80) && (cID < 90))
			{
				TardisPermission p = TardisPermission.get(cID-80);
				return ds.hasPermission(currentPerson, p) ? 1 : 0;
			}
			if((cID >= 90) && (cID < 100))
			{
				TardisPermission p = TardisPermission.get(cID-90);
				return ds.hasPermission(currentPerson, p) ? 1 : 0;
			}
			if(cID == 100)
				return 1 - visibility;
			if(cID == 130)
				return protectedRadius / (double) maxProtectedRadius;
			if(cID == 132){
				return ds.getSpaceProjection() ? 1 : 0;
			}
		}
		return (float) (((tt + cID) % 40) / 39.0);
	}

	public String getConsoleSetting()
	{
		return consoleSettingString;
	}

	public boolean getInternalOnly()
	{
		return internalOnly;
	}

	public static void refreshAvailableConsoleRooms()
	{
		String[] rooms = TardisMod.schemaHandler.getSchemas(true);
		ArrayList<String> validRooms = new ArrayList<String>(rooms.length);
		for (String room : rooms)
		{
			if (room.startsWith("tardisConsole"))
			{
				validRooms.add(sanitiseConsole(room));
			}
		}
		availableConsoleRooms = validRooms.toArray(new String[validRooms.size()]);
	}

	private static String sanitiseConsole(String c)
	{
		if ((c == null) || (c.length() < 13))
			return c;
		return c.replace("tardisConsole", "");
		// return c.substring(13);
	}

	private static double[] colors = new double[] { 0.2, 0.3, 0.9 };
	@Override
	public double[] getColorRatio(int controlID)
	{
		double[] retVal = { 0, 0, 0 };
		if (controlID == 6)
			retVal = new double[] { 0.2, 0.3, 0.9 };
		if ((controlID >= 50) && (controlID < 60))
		{
			ScrewdriverMode m = ScrewdriverMode.get(controlID - 50);
			return m.c;
		}
		if((controlID >= 90) && (controlID < 100))
			retVal = colors;
		return retVal;
	}

	@Override
	public double getControlHighlight(int controlID)
	{
		if ((controlID >= 10) && (controlID < 20))
		{
			TardisUpgradeMode mode = TardisUpgradeMode.getUpgradeMode(controlID - 10);
			if (mode != null)
				return preparingToUpgrade == mode ? 1 : -1;
		}
		return -1;
	}


	@Override
	public ScrewdriverHelper getScrewHelper(int slot)
	{
		return screwHelper;
	}

	@Override
	public void explode(SimpleCoordStore pos, Explosion explosion)
	{
		TardisDataStore ds = Helper.getDataStore(this);
		if(ds != null)
			ExplosionDamageHelper.damage(ds.damage, pos, explosion, 0.9);
	}

	@Override
	public void writeTransmittable(NBTTagCompound nbt)
	{
		super.writeTransmittable(nbt);
		if (screwHelper != null) screwHelper.writeToNBT(nbt, "sNBT");
		if (currentPerson != null)
			nbt.setString("cP", currentPerson);
		if (preparingToUpgrade != null)
			nbt.setInteger("ptU", preparingToUpgrade.ordinal());
		else
			nbt.setBoolean("ptUN", false);
		nbt.setBoolean("io", internalOnly);
		nbt.setInteger("lB", lastButton);
		nbt.setBoolean("eO", isEngineOpen);
		nbt.setInteger("sp", protectedRadius);
	}

	@Override
	public void readTransmittable(NBTTagCompound nbt)
	{
		super.readTransmittable(nbt);
		screwHelper = ScrewdriverHelperFactory.get(nbt,"sNBT");
		currentPerson = nbt.getString("cP");
		lastButton = nbt.getInteger("lB");
		lastButtonTT = tt;
		preparingToUpgradeTT = tt;
		internalOnly = nbt.getBoolean("io");
		isEngineOpen = nbt.getBoolean("eO");
		if (nbt.hasKey("ptU"))
			preparingToUpgrade = TardisUpgradeMode.getUpgradeMode(nbt.getInteger("ptU"));
		else if (nbt.hasKey("ptUN"))
			preparingToUpgrade = null;
		protectedRadius = nbt.getInteger("sp");
	}

	@Override
	public void readTransmittableOnly(NBTTagCompound nbt)
	{
		if (nbt.hasKey("css"))
			consoleSettingString = nbt.getString("css");
		else
			consoleSettingString = "Main";
	}

	@Override
	public void writeTransmittableOnly(NBTTagCompound nbt)
	{
		nbt.setString("css", sanitiseConsole(consoleSettingString));
	}

	public String[] getExtraInfo(int control)
	{
		TardisDataStore ds = Helper.getDataStore(this);
		switch(control)
		{
			case 60: return new String[] { "Current mode: " + (internalOnly ? "Only interfaces inside the TARDIS can interact" : "All interfaces can interact") };
		}
		if((control >= 101) && (control <= 108))
		{
			if(ds != null)
			{
				int slot = control - 101;
				AbstractUpgrade up = ds.upgrades[slot];
				if(up != null)
					return up.getExtraInfo();
			}
		}
		if ((control >= 20) && (control < 30) && (ds != null))
		{
			TardisUpgradeMode mode = TardisUpgradeMode.getUpgradeMode(control - 20);
			return new String[] {"Level: " + ds.getLevel(mode) + "("+ds.getLevel(mode, true)+")"};
		}
		else if((control >= 90) && (control < 100) && (ds != null))
		{
			TardisPermission p = TardisPermission.get(control - 90);
			return new String[]{ currentPerson + (ds.hasPermission(currentPerson, p) ? hasPerm : hasNoPerm) + p.name};
		}
		else if((control >= 110) && (control < 120) && (ds != null))
		{
			return new String[]{ String.format("Repairing will take %s", TardisDamageSystem.repairCompNames[control-110]) };
		}
		else if(control == 130)
			return new String[]{ String.format("Protection radius: %d blocks", getProtectedSpawnRadius()) };
		else if(control == 132)
			return new String[]{ String.format("Current style: " + (ds.getSpaceProjection() ? "Space" : "Overworld")) };
		return null;
	}

	public int getProtectedSpawnRadius()
	{
		TardisDataStore ds = Helper.getDataStore(this);
		if((ds != null) && ds.hasFunction(TardisFunction.SPAWNPROT))
			return protectedRadius;
		return 0;
	}
}
