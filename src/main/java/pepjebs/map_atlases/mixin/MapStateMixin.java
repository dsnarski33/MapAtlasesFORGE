package pepjebs.map_atlases.mixin;

import com.google.common.collect.Maps;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pepjebs.map_atlases.utils.MapStateIntrfc;

import java.util.Map;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MapStateMixin implements MapStateIntrfc {
    @Final @Shadow Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();

    public Map<String, MapDecoration> getFullIcons(){
        return decorations;
    }
}
