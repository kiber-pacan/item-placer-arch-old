package com.akicater;

import com.akicater.blocks.layingItem;
import com.akicater.blocks.layingItemBlockEntity;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ItemPlacer {
    public static final Logger LOGGER = LoggerFactory.getLogger("itemplacer");
    public static final String MODID = "itemplacer";

    public static final Identifier ITEMPLACE = new Identifier(MODID, "itemplace");
    public static final Identifier ITEMROTATE= new Identifier(MODID, "itemrotate");


    //public static final layingItem LAYING_ITEM = new layingItem(Block.Settings.create().breakInstantly().nonOpaque().noBlockBreakParticles().pistonBehavior(PistonBehavior.DESTROY));

    public static final Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(MODID));

    static Registrar<Block> BLOCKS = MANAGER.get().get(Registries.BLOCK);
    public static RegistrySupplier<layingItem> LAYING_ITEM = BLOCKS.register(new Identifier(MODID, "laying_item"), () -> new layingItem(Block.Settings.create().breakInstantly().nonOpaque().noBlockBreakParticles().pistonBehavior(PistonBehavior.DESTROY)));

    static Registrar<BlockEntityType<?>> BLOCK_ENTITIES = MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE);
    public static RegistrySupplier<BlockEntityType<layingItemBlockEntity>> LAYING_ITEM_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            new Identifier(MODID, "laying_item_block_entity"),
            () -> BlockEntityType.Builder.create(layingItemBlockEntity::new, LAYING_ITEM.get()).build(null)
    );

    public static final KeyBinding STOP_SCROLLING_KEY = new KeyBinding(
            "key.examplemod.stop_scrolling",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "Rotate item"
    );

    public static final KeyBinding PLACE_ITEM = new KeyBinding(
            "key.examplemod.place_item",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "Place item"
    );



    public static void initialize() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ITEMPLACE, (buf, context) -> {
            PlayerEntity player = context.getPlayer();
            ItemStack stack = player.getMainHandStack();
            World world = player.getEntityWorld();
            BlockPos pos = buf.readBlockPos();
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                Direction dir = buf.readBlockHitResult().getSide().getOpposite();
                BlockState state = LAYING_ITEM.get().getDefaultState();
                world.setBlockState(pos, state);
                state.initShapeCache();
                layingItemBlockEntity blockEntity = (layingItemBlockEntity)world.getChunk(pos).getBlockEntity(pos);
                if (blockEntity != null) {
                    int i = dirToInt(dir);
                    blockEntity.directions.list.set(i, true);
                    blockEntity.inventory.set(i, stack);
                    world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    blockEntity.markDirty();
                }
            } else if (world.getChunk(pos).getBlockState(pos).getBlock() == LAYING_ITEM.get()) {
                Direction dir = buf.readBlockHitResult().getSide().getOpposite();
                layingItemBlockEntity blockEntity = (layingItemBlockEntity)world.getChunk(pos).getBlockEntity(pos);
                if (blockEntity != null) {
                    int i = dirToInt(dir);
                    if(blockEntity.inventory.get(i).isEmpty()) {
                        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        blockEntity.directions.list.set(i, true);
                        blockEntity.inventory.set(i, stack);
                        world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                        blockEntity.markDirty();
                    }
                }
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ITEMROTATE, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            World world = context.getPlayer().getEntityWorld();
            BlockEntity blockEntity = world.getChunk(pos).getBlockEntity(pos);
            if (blockEntity instanceof layingItemBlockEntity) {
                ((layingItemBlockEntity) blockEntity).rotate(buf.readFloat(), getDirection(buf.readBlockHitResult()));
            }
        });
        EnvExecutor.runInEnv(Env.CLIENT, () -> Client::initializeClient);
    }

    @Environment(EnvType.CLIENT)
    public static class Client {
        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            //BlockEntityRendererRegistry.register(LAYING_ITEM_BLOCK_ENTITY.get(), layingItemBER::new);


            NetworkManager.registerReceiver(NetworkManager.Side.S2C, ITEMPLACE, (buf, context) -> {
                PlayerEntity player = context.getPlayer();
                ItemStack stack = player.getMainHandStack();
                World world = player.getEntityWorld();
                BlockPos pos = buf.readBlockPos();
                if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                    player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    Direction dir = buf.readBlockHitResult().getSide().getOpposite();
                    BlockState state = LAYING_ITEM.get().getDefaultState();
                    world.setBlockState(pos, state);
                    state.initShapeCache();
                    layingItemBlockEntity blockEntity = (layingItemBlockEntity)world.getChunk(pos).getBlockEntity(pos);
                    if (blockEntity != null) {
                        int i = dirToInt(dir);
                        blockEntity.directions.list.set(i, true);
                        blockEntity.inventory.set(i, stack);
                        world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                        blockEntity.markDirty();
                    }
                } else if (world.getBlockState(pos).getBlock() == LAYING_ITEM.get()) {
                    Direction dir = buf.readBlockHitResult().getSide().getOpposite();
                    layingItemBlockEntity blockEntity = (layingItemBlockEntity)world.getChunk(pos).getBlockEntity(pos);
                    if (blockEntity != null) {
                        int i = dirToInt(dir);
                        if(blockEntity.inventory.get(i).isEmpty()) {
                            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                            blockEntity.directions.list.set(i, true);
                            blockEntity.inventory.set(i, stack);
                            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                            blockEntity.markDirty();
                        }
                    }
                }
            });

            NetworkManager.registerReceiver(NetworkManager.Side.C2S, ITEMROTATE, (buf, context) -> {
                BlockPos pos = buf.readBlockPos();
                World world = context.getPlayer().getEntityWorld();
                BlockEntity blockEntity = world.getChunk(pos).getBlockEntity(pos);
                if (blockEntity instanceof layingItemBlockEntity) {
                    ((layingItemBlockEntity) blockEntity).rotate(buf.readFloat(), getDirection(buf.readBlockHitResult()));
                }
            });
            ClientTickEvent.CLIENT_POST.register(client -> {
                if (PLACE_ITEM.wasPressed()) {
                    if (client.crosshairTarget instanceof BlockHitResult && client.player.getStackInHand(Hand.MAIN_HAND) != ItemStack.EMPTY && MinecraftClient.getInstance().world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock() != Blocks.AIR) {
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        Direction side = ((BlockHitResult) client.crosshairTarget).getSide();
                        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
                        buf.writeBlockPos(pos.offset(side, 1));
                        buf.writeBlockHitResult((BlockHitResult) client.crosshairTarget);
                        NetworkManager.sendToServer(ITEMPLACE, buf);
                    }
                }
            });
        }
    }

    public static int dirToInt(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case NORTH -> 1;
            case EAST -> 2;
            case WEST -> 3;
            case UP -> 4;
            case DOWN -> 5;
        };
    }

    public static Direction intToDir(int dir) {
        return switch (dir) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            case 3 -> Direction.WEST;
            case 4 -> Direction.UP;
            case 5 -> Direction.DOWN;
            default -> throw new IllegalStateException("Unexpected value: " + dir);
        };
    }

    static boolean contains(Vec3d vec, Box box) {
        return vec.x >= box.minX
                && vec.x <= box.maxX
                && vec.y >= box.minY
                && vec.y <= box.maxY
                && vec.z >= box.minZ
                && vec.z <= box.maxZ;
    }

    public static int getDirection(BlockHitResult hit) {
        double xT = hit.getPos().getX();
        double yT = hit.getPos().getY();
        double zT = hit.getPos().getZ();

        double x = (xT > 0) ? xT - ((int)xT) : 1 - Math.abs(xT - ((int)xT));
        double y = (yT > 0) ? yT - ((int)yT) : 1 - Math.abs(yT - ((int)yT));
        double z = (zT > 0) ? zT - ((int)zT) : 1 - Math.abs(zT - ((int)zT));

        Vec3d pos = new Vec3d(x,y,z);
        List<Box> boxes = new ArrayList<>(
                List.of(
                        new Box(0.125f, 0.125f, 0.875f, 0.875f, 0.875f, 1.0f),
                        new Box(0.125f, 0.125f, 0.0f, 0.875f, 0.875f, 0.125f),
                        new Box(0.875f, 0.125f, 0.125f, 1.0f, 0.875f, 0.875f),
                        new Box(0.0f, 0.125f, 0.125f, 0.125f, 0.875f, 0.875f),
                        new Box(0.125f, 0.875f, 0.125f, 0.875f, 1.0f, 0.875f),
                        new Box(0.125f, 0.0f, 0.125f, 0.875f, 0.125f, 0.875f)
                )
        );
        for (int i = 0; i < boxes.size(); i++) {
            if (contains(pos, boxes.get(i))) {
                return i;
            }
        }
        return 0;
    }
}