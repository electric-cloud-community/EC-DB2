def thisLoader = this.class.classLoader
// try the "proper" way to find the root classloader
def rootLoader = this.class.getClassLoader().getRootLoader();
if (rootLoader == null) {
    // Root classloader is not a groovy RootLoader, but we still need it,
    // so walk up the hierarchy and get the top one (whose parent is null)
    // When running from Java this is sun.misc.Launcher.ExtClassLoader
    rootLoader = thisLoader
    ClassLoader parentLoader = rootLoader.getParent()
    while (parentLoader != null) {
        rootLoader = parentLoader
        parentLoader = parentLoader.getParent()
    }
}
public class ECClassLoader {
    def cachedRootLoader;

    public def loadClass(jarPath, jarClass) {
        cachedRootLoader.addURL(new File(jarPath).toURL())
        def classInstance = 
        Class.forName(jarClass,
                      true,
                      cachedRootLoader).newInstance();
    }
    public def loadClass(jarPath) {
        cachedRootLoader.addURL(new File(jarPath).toURL())
        return true;
    }
}
ecl = new ECClassLoader(cachedRootLoader: rootLoader);

