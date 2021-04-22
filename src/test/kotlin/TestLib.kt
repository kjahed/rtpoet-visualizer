import ca.jahed.rtpoet.rtmodel.*
import ca.jahed.rtpoet.rtmodel.sm.RTPseudoState
import ca.jahed.rtpoet.rtmodel.sm.RTState
import ca.jahed.rtpoet.rtmodel.sm.RTStateMachine
import ca.jahed.rtpoet.rtmodel.sm.RTTransition
import ca.jahed.rtpoet.rtmodel.types.primitivetype.RTInt
import ca.jahed.rtpoet.visualizer.RTVisualizer
import org.junit.jupiter.api.Test

class TestLib {

    private fun createPingerPonger(): RTModel {
        val ppProtocol =
            RTProtocol.builder("PPProtocol")
                .output(RTSignal.builder("ping").parameter(RTParameter.builder("round", RTInt)))
                .input(RTSignal.builder("pong").parameter(RTParameter.builder("round", RTInt)))
                .build()

        val pinger =
            RTCapsule.builder("Pinger")
                .attribute(RTAttribute.builder("count", RTInt))
                .port(RTPort.builder("ppPort", ppProtocol).external())
                .statemachine(
                    RTStateMachine.builder()
                        .state(RTPseudoState.initial("initial"))
                        .state(RTState.builder("playing"))
                        .transition(
                            RTTransition.builder("initial", "playing")
                                .action("""
                                this->count = 1;
                                ppPort.ping(count).send();
                            """)
                        )
                        .transition(
                            RTTransition.builder("playing", "playing")
                                .trigger("ppPort", "pong")
                                .action("""
                               ppPort.ping(++count).send();
                            """)
                        )
                )
                .build()

        val ponger =
            RTCapsule.builder("Ponger")
                .port(RTPort.builder("ppPort", ppProtocol).external().conjugate())
                .statemachine(
                    RTStateMachine.builder()
                        .state(RTPseudoState.initial("initial"))
                        .state(RTState.builder("playing"))
                        .transition(RTTransition.builder("initial", "playing"))
                        .transition(
                            RTTransition.builder("playing", "playing")
                                .trigger("ppPort", "ping")
                                .action("""
                               ppPort.pong(round++).send();
                            """)
                        )
                )
                .build()

        val top =
            RTCapsule.builder("Top")
                .part(RTCapsulePart.builder("pinger", pinger))
                .part(RTCapsulePart.builder("ponger", ponger))
                .connector(RTConnector.builder()
                    .end1(RTConnectorEnd.builder("ppPort", "pinger"))
                    .end2(RTConnectorEnd.builder("ppPort", "ponger"))
                )
                .build()

        return RTModel.builder("PingerPonger", top)
            .capsule(pinger)
            .capsule(ponger)
            .protocol(ppProtocol)
            .build()
    }

    @Test
    internal fun TestVisualizer() {
        val rtModel = createPingerPonger();
        RTVisualizer.draw(rtModel);
    }
}