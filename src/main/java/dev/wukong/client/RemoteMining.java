package dev.wukong.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** External-server mining. Uses only the ordinary client game-mode mining API.
 * No local world writes, packet construction, or server-side inventory changes. */
public final class RemoteMining {
    private record Candidate(BlockPos pos, BlockState original, Direction face, int slot) {}
    private record Precision(BlockPos anchor, BlockState original, MiningShape.Face face,
            int width,int height,int depth,boolean same,WorldMining.Filter filter) {}
    private static Object player,level,gameMode,profile;
    private static Precision observed, job;
    private static final List<BlockPos> remaining=new ArrayList<>();
    private static Candidate active, verifying;
    private static boolean precisionActive;
    private static int previousSlot=-1, ownedSlot=-1, age, settle, cooldown, batch;
    private static boolean areaBlocked, precisionBlocked;
    private static final int SETTLE_TICKS=8, MAX_MINE_TICKS=600;

    static boolean ownsHotbar(){return ownedSlot>=0;}
    private static boolean allowed(Minecraft mc){
        return WorldMining.commonAllowed(mc)&&!mc.hasSingleplayerServer()
            &&mc.gameMode.getPlayerMode().isSurvival()&&!mc.player.isUsingItem()
            &&!mc.options.keyUse.isDown()&&mc.player.containerMenu==mc.player.inventoryMenu
            &&mc.player.inventoryMenu.getCarried().isEmpty();
    }
    private static boolean areaEnabled(Minecraft mc){
        var c=BenchConfig.INSTANCE;
        return c.worldMiningMaster&&c.areaNuker&&c.areaMultiplayer&&(!c.areaSneak||mc.player.isShiftKeyDown());
    }
    private static boolean precisionEnabled(Minecraft mc){
        var c=BenchConfig.INSTANCE;
        return c.worldMiningMaster&&c.precisionNuker&&c.precisionMultiplayer&&(!c.precisionSneak||mc.player.isShiftKeyDown());
    }
    private static boolean autoTool(){var c=BenchConfig.INSTANCE;return c.survivalMasterEnabled&&c.autoToolEnabled;}
    private static int reserve(){var c=BenchConfig.INSTANCE;return c.survivalMasterEnabled&&c.toolSaverEnabled?MiningShape.clamp(c.toolSaverMinDurability,1,100):1;}
    private static WorldMining.Filter areaFilter(){var c=BenchConfig.INSTANCE;return WorldMining.filter(c.areaPreserveOres,c.areaInclude,c.areaExclude);}
    private static WorldMining.Filter precisionFilter(){var c=BenchConfig.INSTANCE;return WorldMining.filter(c.precisionPreserveOres,c.precisionInclude,c.precisionExclude);}
    private static boolean sameWorld(Minecraft mc){return player==mc.player&&level==mc.level&&gameMode==mc.gameMode&&profile==BenchConfig.INSTANCE;}
    private static void bind(Minecraft mc){
        if(sameWorld(mc))return;
        // Never send an abort or restore a slot into a newly connected world.
        if(player==mc.player&&level==mc.level&&gameMode==mc.gameMode)stop(mc);
        clear();areaBlocked=precisionBlocked=false;cooldown=0;batch=0;
        player=mc.player;level=mc.level;gameMode=mc.gameMode;profile=BenchConfig.INSTANCE;
    }
    private static void clear(){observed=job=null;remaining.clear();active=verifying=null;previousSlot=ownedSlot=-1;age=settle=0;precisionActive=false;}
    private static void release(Minecraft mc){
        if(mc.player!=null&&mc.player==player&&ownedSlot>=0&&mc.player.getInventory().getSelectedSlot()==ownedSlot&&previousSlot>=0){
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot=ownedSlot=-1;
    }
    private static void stop(Minecraft mc){
        if(active!=null&&mc.gameMode!=null&&mc.gameMode==gameMode)mc.gameMode.stopDestroyBlock();
        release(mc);clear();batch=0;
    }
    static void cleanup(Minecraft mc,boolean gameplay){
        bind(mc);
        if(!gameplay||!allowed(mc)){stop(mc);return;}
        if(!areaEnabled(mc)){areaBlocked=false;if(!precisionActive&&(active!=null||verifying!=null))stop(mc);}
        if(!precisionEnabled(mc)){precisionBlocked=false;observed=null;if(job!=null||precisionActive)stop(mc);}
    }
    public static void beforeManualAttack(Minecraft mc){
        bind(mc);
        if(active!=null||job!=null||verifying!=null)stop(mc);
        if(allowed(mc)&&precisionEnabled(mc)&&!precisionBlocked)observe(mc);
    }
    private static void observe(Minecraft mc){
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK){observed=null;return;}
        var c=BenchConfig.INSTANCE;BlockPos pos=hit.getBlockPos().immutable();
        if(observed!=null&&observed.anchor().equals(pos))return;
        var state=mc.level.getBlockState(pos);var f=precisionFilter();
        if(!WorldMining.allowedBlock(mc.level,pos,state,f)){observed=null;return;}
        observed=new Precision(pos,state,MiningShape.Face.valueOf(hit.getDirection().name()),c.precisionWidth,c.precisionHeight,c.precisionDepth,c.precisionSameBlock,f);
    }
    private static boolean currentPrecision(Minecraft mc,Precision p){
        var c=BenchConfig.INSTANCE;
        return precisionEnabled(mc)&&p.width()==c.precisionWidth&&p.height()==c.precisionHeight&&p.depth()==c.precisionDepth&&p.same()==c.precisionSameBlock&&p.filter().equals(precisionFilter());
    }
    private static void startJob(Precision p){
        job=p;observed=null;remaining.clear();precisionActive=true;
        for(var o:MiningShape.precision(p.width(),p.height(),p.depth(),p.face())){
            if(o.x()!=0||o.y()!=0||o.z()!=0)remaining.add(p.anchor().offset(o.x(),o.y(),o.z()));
        }
        // Client-side prediction is not an explicit server acknowledgement. Wait for updates.
        settle=SETTLE_TICKS;
    }
    private static boolean inArea(Minecraft mc,BlockPos pos){
        var c=BenchConfig.INSTANCE;BlockPos feet=mc.player.blockPosition(),head=feet.above();
        int floor=MiningShape.floorStart(mc.player.getY());
        BlockPos center=c.areaProtectFloor&&!c.areaLayer?new BlockPos(feet.getX(),floor,feet.getZ()):head;
        int x=pos.getX()-center.getX(),y=pos.getY()-center.getY(),z=pos.getZ()-center.getZ(),r=MiningShape.clamp(c.areaRadius,1,5);
        if(Math.abs(x)>r||Math.abs(y)>r||Math.abs(z)>r||(c.areaLayer&&y!=0)||(c.areaProtectFloor&&pos.getY()<floor))return false;
        if(c.areaSphere&&x*x+y*y+z*z>r*r)return false;
        return !(pos.getX()==head.getX()&&pos.getZ()==head.getZ()&&pos.getY()>=head.getY()-2&&pos.getY()<=head.getY());
    }
    private static Direction visibleFace(Minecraft mc,BlockPos pos){
        var eye=mc.player.getEyePosition();
        var center=new Vec3(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        var hit=mc.level.clip(new ClipContext(eye,center,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player));
        return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos)?hit.getDirection():null;
    }
    private static Candidate candidate(Minecraft mc,BlockPos pos,boolean precision){
        if(!mc.level.hasChunkAt(pos)||mc.level.isOutsideBuildHeight(pos)||!mc.level.getWorldBorder().isWithinBounds(pos)||!mc.player.isWithinBlockInteractionRange(pos,0.0))return null;
        if(!precision&&!inArea(mc,pos))return null;
        var state=mc.level.getBlockState(pos);
        if(precision&&(job==null||(job.same()&&job.original().getBlock()!=state.getBlock())))return null;
        if(!WorldMining.allowedBlock(mc.level,pos,state,precision?job.filter():areaFilter()))return null;
        var face=visibleFace(mc,pos);if(face==null)return null;
        int slot=WorldMining.toolSlot(mc.player,state,autoTool(),reserve());
        return slot<0?null:new Candidate(pos,state,face,slot);
    }
    private static Candidate next(Minecraft mc,boolean precision){
        if(precision){
            remaining.removeIf(p->mc.level.hasChunkAt(p)&&mc.level.getBlockState(p).isAir());
            for(var pos:remaining){var c=candidate(mc,pos,true);if(c!=null)return c;}
            return null;
        }
        var c=BenchConfig.INSTANCE;var feet=mc.player.blockPosition();var head=feet.above();
        var center=c.areaProtectFloor&&!c.areaLayer?new BlockPos(feet.getX(),MiningShape.floorStart(mc.player.getY()),feet.getZ()):head;
        for(var o:MiningShape.area(c.areaRadius,c.areaSphere,c.areaLayer,c.areaProtectFloor)){
            var q=candidate(mc,center.offset(o.x(),o.y(),o.z()),false);if(q!=null)return q;
        }
        return null;
    }
    private static boolean begin(Minecraft mc,Candidate target,boolean precision){
        if(!SurvivalAutomation.prepareRemoteMining(mc))return false;
        // Auto Tool may have restored a prior manually selected slot. Re-evaluate.
        target=candidate(mc,target.pos(),precision);if(target==null)return false;
        var inv=mc.player.getInventory();previousSlot=inv.getSelectedSlot();ownedSlot=target.slot();
        inv.setSelectedSlot(ownedSlot);active=target;precisionActive=precision;age=0;
        if(!mc.gameMode.startDestroyBlock(target.pos(),target.face())){failed(mc,precision);return false;}
        return true;
    }
    private static void failed(Minecraft mc,boolean precision){
        stop(mc);
        if(precision)precisionBlocked=true;else areaBlocked=true;
        EvolutionUi.note(ModuleCatalog.tr("Nuker pausiert: Abbau nicht bestätigt. Modul aus/ein zum Fortsetzen.","Nuker paused: block break not confirmed. Toggle module to resume."));
    }
    /** Called by the normal vanilla attack loop, at most once per gameplay tick. */
    public static boolean tickAttack(Minecraft mc,boolean down){
        bind(mc);
        if(!allowed(mc)){stop(mc);return false;}
        boolean area=areaEnabled(mc),precision=precisionEnabled(mc);
        if(!area)areaBlocked=false;
        if(!precision)precisionBlocked=false;
        if(!area&&!precision){stop(mc);return false;}
        if(cooldown>0)cooldown--;
        if((job!=null&&!currentPrecision(mc,job))||(!precisionActive&&(active!=null||verifying!=null)&&(!area||down))){stop(mc);}
        if(job==null&&precision&&!precisionBlocked&&observed!=null){
            if(currentPrecision(mc,observed)&&mc.level.hasChunkAt(observed.anchor())&&mc.level.getBlockState(observed.anchor()).isAir())startJob(observed);
            else if(!down)observed=null;
        }
        if(job==null&&down){if(precision&&!precisionBlocked)observe(mc);return false;}
        if(active!=null){
            var a=active;
            if(mc.player.getInventory().getSelectedSlot()!=ownedSlot){stop(mc);cooldown=10;return false;}
            if(!mc.level.hasChunkAt(a.pos())){stop(mc);return false;}
            if(mc.level.getBlockState(a.pos()).isAir()){
                verifying=a;active=null;release(mc);settle=SETTLE_TICKS;return true;
            }
            var live=candidate(mc,a.pos(),precisionActive);
            if(live==null||live.original().getBlock()!=a.original().getBlock()||live.slot()!=ownedSlot){stop(mc);cooldown=10;return false;}
            if(++age>MAX_MINE_TICKS){failed(mc,precisionActive);return false;}
            mc.gameMode.continueDestroyBlock(a.pos(),live.face());return true;
        }
        if(settle>0){settle--;return true;}
        if(verifying!=null){
            if(!mc.level.hasChunkAt(verifying.pos())||!mc.level.getBlockState(verifying.pos()).isAir()){failed(mc,precisionActive);return false;}
            if(precisionActive)remaining.remove(verifying.pos());
            else if(++batch>=MiningShape.clamp(BenchConfig.INSTANCE.areaBatch,1,12)){batch=0;cooldown=MiningShape.clamp(BenchConfig.INSTANCE.areaInterval,1,40);}
            verifying=null;
        }
        if(job!=null){
            if(!mc.level.hasChunkAt(job.anchor())||!mc.level.getBlockState(job.anchor()).isAir()){failed(mc,true);return false;}
            var n=next(mc,true);
            if(n==null){job=null;remaining.clear();precisionActive=false;return false;}
            return begin(mc,n,true);
        }
        if(!area||areaBlocked||cooldown>0||down)return false;
        var n=next(mc,false);return n!=null&&begin(mc,n,false);
    }
    private RemoteMining(){}
}
