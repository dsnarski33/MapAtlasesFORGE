/**
 * This class was pulled and reduced from: https://github.com/JOML-CI/JOML
 * Under the MIT license.
 */
package pepjebs.map_atlases.utils;

import com.mojang.math.Quaternion;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@FunctionalInterface
public interface RotationAxis
{
//      static RotationAxis of(Vector3f axis) {
//        return (rad) -> {
//            return (new Quaternionf()).rotationAxis(rad, axis);
//        };
//    }
//    RotationAxis NEGATIVE_X = (rad) -> (new Quaternionf()).rotationX(-rad);
//    RotationAxis POSITIVE_X = (rad) -> (new Quaternionf()).rotationX(rad);
//    RotationAxis NEGATIVE_Y = (rad) -> (new Quaternionf()).rotationY(-rad);
//    RotationAxis POSITIVE_Y = (rad) -> (new Quaternionf()).rotationY(rad);
//    RotationAxis NEGATIVE_Z = (rad) -> (new Quaternionf()).rotationZ(-rad);

    RotationAxis POSITIVE_Z = (rad) -> (new Quaternionf()).rotationZ(rad);
    Quaternionf rotation(float rad);

    default Quaternionf rotationDegreesf(float deg) { return this.rotation(deg * 0.017453292F); }
    default Quaternion rotationDegrees(float deg) { return this.toQuaternion(this.rotation(deg * 0.017453292F)); }
    default Quaternion toQuaternion(Quaternionf quaternion) {  return new Quaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w); }
}