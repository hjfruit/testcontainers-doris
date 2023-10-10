package testcontainers.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class SimpleDorisCluster {

    private static final Logger log = LoggerFactory.getLogger(SimpleDorisCluster.class);

    public static void main(String[] args) {
        testDorisContainerCluster();
    }


    public static void testDorisContainerCluster() {

        try (DorisClusterContainer cluster = new Doris1FE1BEClusterContainer()) {
            cluster.start();
            GenericContainer<?> be = cluster.getBeList().get(0);
            GenericContainer<?> fe = cluster.geFeList().get(0);
            int instanceIndex = 1;

            assert (be.getContainerName().startsWith(Doris.beName() + instanceIndex));
            assert (fe.getContainerName().startsWith(Doris.feName() + instanceIndex));

            assert cluster.getFePortList().contains(Doris.feHttpPort());
            assert cluster.getBePortList().get(0) == Doris.beExposedPort();
            assert cluster.beList().head().getDependencies().isEmpty();
            assert cluster.feList().head().getDependencies().size() == 1;

            if (cluster.existsRunningContainer()) {
                cluster.stop();
            }
        }

    }
}
