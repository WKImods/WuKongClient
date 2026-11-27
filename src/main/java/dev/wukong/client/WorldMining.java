package dev.wukong.client;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Local-server excavation and common mining selection. RemoteMining uses normal client interactions. */
final class WorldMining {
    record Filter(boolean preserveOres,Set<String> include,Set<String> exclude) {}
    private record Precision(BlockPos anchor,MiningShape.Face face,int width,int height,int depth,boolean same,Filter filter) {}
    private record Area(int radius,boolean sphere,boolean layer,boolean protectFloor,int interval,int batch,Filter filter) {}
    private record Request(MinecraftServer server,UUID id,long session,Precision precision,Area area,int reserve,boolean autoTool) {}
    private static volatile Request request;
    private static Object clientPlayer,clientLevel;
    private static long session;
    private static BlockPos attackAnchor;
    private static MiningShape.Face attackFace;
    private static Object previousServer,previousOwner,previousLevel;
    private static long previousSession=-1;
    private static int areaCooldown;
    private static boolean busy;

    static void install(){
        AttackBlockCallback.EVENT.register((p,l,hand,pos,face)->{
            if(!l.isClientSide())return InteractionResult.PASS;
            var mc=Minecraft.getInstance();
            if(mc.player==p&&hand==InteractionHand.MAIN_HAND&&clientAllowed(mc)&&!BenchConfig.INSTANCE.precisionMultiplayer){
                attackAnchor=pos.immutable();attackFace=MiningShape.Face.valueOf(face.name());
                publish(mc,true);
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity)->{
            if(busy||!(player instanceof ServerPlayer sp))return;
            var r=request;
            if(r==null||r.precision()==null||!owns(r,sp)||!r.precision().anchor().equals(pos))return;
            // Run on the server tick after the original break; never recursively call the same event.
            pending=new Pending(r,sp,level,state);
        });
        ServerTickEvents.END_SERVER_TICK.register(WorldMining::serverTick);
    }
    private record Pending(Request request,ServerPlayer player,Level level,BlockState original) {}
    private static Pending pending;
    static boolean commonAllowed(Minecraft mc){return mc.player!=null&&mc.level!=null&&mc.gameMode!=null&&mc.gui.screen()==null&&!mc.isPaused()&&mc.player.isAlive()&&!mc.player.isSpectator()&&!mc.player.hasInfiniteMaterials();}
    private static boolean clientAllowed(Minecraft mc){return commonAllowed(mc)&&mc.hasSingleplayerServer()&&mc.getSingleplayerServer()!=null;}
    private static Set<String> ids(String input){
        if(input==null||input.isBlank())return Set.of();
        return Arrays.stream(input.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(s->!s.isEmpty()).map(s->s.contains(":")?s:"minecraft:"+s).collect(Collectors.toUnmodifiableSet());
    }
    static Filter filter(boolean ores,String include,String exclude){return new Filter(ores,ids(include),ids(exclude));}
    static void publish(Minecraft mc,boolean gameplay){
        RemoteMining.cleanup(mc,gameplay);
        if(clientPlayer!=mc.player||clientLevel!=mc.level){clientPlayer=mc.player;clientLevel=mc.level;session++;attackAnchor=null;attackFace=null;}
        var c=BenchConfig.INSTANCE;
        if(!gameplay||!clientAllowed(mc)||!c.worldMiningMaster||mc.player.isUsingItem()||mc.options.keyUse.isDown()){
            request=null;attackAnchor=null;attackFace=null;return;
        }
        boolean attack=mc.options.keyAttack.isDown();
        boolean sneak=mc.player.isShiftKeyDown();
        Precision precision=null;
        if(c.precisionNuker&&!c.precisionMultiplayer&&(!c.precisionSneak||sneak)&&mc.hitResult instanceof BlockHitResult hit&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){
            // Update for continued mining, including vanilla's held-button next block.
            attackAnchor=hit.getBlockPos().immutable();attackFace=MiningShape.Face.valueOf(hit.getDirection().name());
        }
        if(c.precisionNuker&&!c.precisionMultiplayer&&attackAnchor!=null&&attackFace!=null&&(!c.precisionSneak||sneak)){
            precision=new Precision(attackAnchor,attackFace,c.precisionWidth,c.precisionHeight,c.precisionDepth,c.precisionSameBlock,filter(c.precisionPreserveOres,c.precisionInclude,c.precisionExclude));
        }else{attackAnchor=null;attackFace=null;}
        Area area=c.areaNuker&&!c.areaMultiplayer&&!attack&&(!c.areaSneak||sneak)?new Area(c.areaRadius,c.areaSphere,c.areaLayer,c.areaProtectFloor,c.areaInterval,c.areaBatch,filter(c.areaPreserveOres,c.areaInclude,c.areaExclude)):null;
        int reserve=c.survivalMasterEnabled&&c.toolSaverEnabled?MiningShape.clamp(c.toolSaverMinDurability,1,100):1;
        request=precision==null&&area==null?null:new Request(mc.getSingleplayerServer(),mc.player.getUUID(),session,precision,area,reserve,c.survivalMasterEnabled&&c.autoToolEnabled);
    }
    private static boolean sameOptions(Precision a,Precision b){return a.width()==b.width()&&a.height()==b.height()&&a.depth()==b.depth()&&a.same()==b.same()&&a.filter().equals(b.filter());}
    private static boolean owns(Request r,ServerPlayer p){return r.server().getPlayerList().getPlayer(r.id())==p;}
    private static boolean eligible(ServerPlayer p){
        if(!p.gameMode.isSurvival()||!p.isAlive()||p.isSpectator()||p.hasInfiniteMaterials()||p.isUsingItem()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty())return false;
        return true;
    }
    static boolean allowedBlock(Level l,BlockPos pos,BlockState state,Filter f){
        if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity()||state.getDestroySpeed(l,pos)<0)return false;
        if(f.preserveOres()&&OreKind.from(state)!=null)return false;
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return !f.exclude().contains(id)&&(f.include().isEmpty()||f.include().contains(id));
    }
    // Read the same hotbar and use the same mining score as SurvivalAutomation.
    // Never reorder items or ask the client Auto Tool to hold a slot across ticks.
    static int toolSlot(net.minecraft.world.entity.player.Player p,BlockState state,boolean automatic,int reserve){
        var inv=p.getInventory();
        return MiningToolChoice.choose(inv.getSelectedSlot(),automatic,state.requiresCorrectToolForDrops(),reserve,i->{
            ItemStack stack=inv.getItem(i);
            return new MiningToolChoice.Candidate(stack.isEmpty()?1.0f:stack.getDestroySpeed(state),
                !stack.isEmpty()&&stack.isCorrectToolForDrops(state),stack.isDamageableItem(),
                stack.isDamageableItem()?stack.getMaxDamage()-stack.getDamageValue():Integer.MAX_VALUE,
                stack.isEmpty());
        });
    }
    private static boolean reachable(ServerPlayer p,BlockPos pos){return p.isWithinBlockInteractionRange(pos,0.0);}
    private static boolean breakOne(Request r,ServerPlayer p,BlockPos pos,Filter filter,BlockState same){
        if(!eligible(p)||!reachable(p,pos))return false;
        var level=p.level();
        if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(p,pos))return false;
        BlockState state=level.getBlockState(pos);
        if(same!=null&&state.getBlock()!=same.getBlock())return false;
        if(!allowedBlock(level,pos,state,filter))return false;
        int slot=toolSlot(p,state,r.autoTool(),r.reserve());
        if(slot<0)return false;
        var inv=p.getInventory();
        int previous=inv.getSelectedSlot();
        try{
            if(slot!=previous)inv.setSelectedSlot(slot);
            return p.gameMode.destroyBlock(pos);
        }finally{
            // Restore the player's slot even if a modded block throws or cancels.
            if(slot!=previous)inv.setSelectedSlot(previous);
        }
    }
    private static void serverTick(MinecraftServer server){
        var r=request;
        if(r==null||r.server()!=server){pending=null;previousServer=null;previousOwner=null;previousLevel=null;areaCooldown=0;return;}
        var p=server.getPlayerList().getPlayer(r.id());
        if(p==null){pending=null;return;}
        if(previousServer!=server||previousOwner!=p||previousLevel!=p.level()||previousSession!=r.session()){
            previousServer=server;previousOwner=p;previousLevel=p.level();previousSession=r.session();areaCooldown=0;
            if(pending!=null&&(pending.player()!=p||pending.level()!=p.level()||pending.request().session()!=r.session()))pending=null;
        }
        if(areaCooldown>0)areaCooldown--;
        Pending job=pending;pending=null;
        if(!eligible(p))return;
        busy=true;
        try{
            if(job!=null&&job.player()==p&&job.level()==p.level()&&r.precision()!=null&&job.request().session()==r.session()&&sameOptions(job.request().precision(),r.precision())){
                Precision q=job.request().precision();
                // Original block has already been mined by vanilla. Skip it in the batch.
                if(allowedBlock(p.level(),q.anchor(),job.original(),q.filter())){
                    for(var o:MiningShape.precision(q.width(),q.height(),q.depth(),q.face())){
                        if(o.x()==0&&o.y()==0&&o.z()==0)continue;
                        if(!eligible(p))break;
                        breakOne(r,p,q.anchor().offset(o.x(),o.y(),o.z()),q.filter(),q.same()?job.original():null);
                    }
                }
                p.inventoryMenu.broadcastFullState();return;
            }
            Area a=r.area();if(a==null||areaCooldown>0)return;
            areaCooldown=MiningShape.clamp(a.interval(),1,40);
            BlockPos feet=p.blockPosition();
            BlockPos head=feet.above();
            int floorStart=MiningShape.floorStart(p.getY());
            // Protected volume starts at foot height; keep the existing head-height single layer.
            BlockPos center=a.protectFloor()&&!a.layer()?new BlockPos(feet.getX(),floorStart,feet.getZ()):head;
            int budget=MiningShape.clamp(a.batch(),1,12),broken=0;
            for(var o:MiningShape.area(a.radius(),a.sphere(),a.layer(),a.protectFloor())){
                if(broken>=budget||!eligible(p))break;
                BlockPos pos=center.offset(o.x(),o.y(),o.z());
                // Leave the player's feet, head and support block alone.
                if(a.protectFloor()&&pos.getY()<floorStart)continue;
                if(pos.getX()==head.getX()&&pos.getZ()==head.getZ()&&pos.getY()>=head.getY()-2&&pos.getY()<=head.getY())continue;
                if(breakOne(r,p,pos,a.filter(),null))broken++;
            }
            if(broken>0)p.inventoryMenu.broadcastFullState();
        }finally{busy=false;}
    }
    static void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();var c=BenchConfig.INSTANCE;
        if(!commonAllowed(mc)||(c.precisionMultiplayer?mc.hasSingleplayerServer():!clientAllowed(mc))||!c.worldMiningMaster||!c.precisionNuker||!c.precisionPreview||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return;
        if(c.precisionSneak&&!mc.player.isShiftKeyDown())return;
        var camera=mc.gameRenderer.mainCamera();if(camera==null||!camera.isInitialized())return;
        var anchor=hit.getBlockPos();var level=mc.level;var original=level.getBlockState(anchor);
        var f=filter(c.precisionPreserveOres,c.precisionInclude,c.precisionExclude);
        boolean automatic=c.survivalMasterEnabled&&c.autoToolEnabled;
        int reserve=c.survivalMasterEnabled&&c.toolSaverEnabled?MiningShape.clamp(c.toolSaverMinDurability,1,100):1;
        if(!allowedBlock(level,anchor,original,f)||toolSlot(mc.player,original,automatic,reserve)<0)return;
        for(var o:MiningShape.precision(c.precisionWidth,c.precisionHeight,c.precisionDepth,MiningShape.Face.valueOf(hit.getDirection().name()))){
            var pos=anchor.offset(o.x(),o.y(),o.z());if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos))continue;
            var state=level.getBlockState(pos);
            if((c.precisionSameBlock&&state.getBlock()!=original.getBlock())||!allowedBlock(level,pos,state,f)||toolSlot(mc.player,state,automatic,reserve)<0||!mc.player.isWithinBlockInteractionRange(pos,0.0))continue;
            var box=new AABB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1);
            EspOverlay.drawMarker(g,mc,camera,box,"",0,0xFFFFB54A,c,true,false,false);
        }
    }
    private WorldMining(){}
}
