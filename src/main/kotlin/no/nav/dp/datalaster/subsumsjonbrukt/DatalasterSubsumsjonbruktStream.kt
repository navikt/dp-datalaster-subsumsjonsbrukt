package no.nav.dp.datalaster.subsumsjonbrukt

import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import java.util.Properties

class DatalasterSubsumsjonbruktStream(
    private val subsumsjonApiClient: SubsumsjonApiClient,
    private val configuration: Configuration
) : Service() {
    override val SERVICE_APP_ID: String = "dp-datalaster-subsumsjonbrukt"
    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        builder
            .consumeTopic(inTopic)
            .mapValues { _, jsonValue -> SubsumsjonId.fromJson(jsonValue) }
            .mapValues { _, id -> id?.let { subsumsjonApiClient.subsumsjon(it) } }
            .filterNot { _, value -> value == null }
            .toTopic(outTopic)
        return builder.build()
    }

    override fun getConfig(): Properties {
        return streamConfig(
            SERVICE_APP_ID, configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.application.username, configuration.application.password)
        ).also {
            it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
    }
}

val outTopic = Topic(
    "privat-dagpenger-subsumsjon-brukt-data",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)

val inTopic = Topic(
    "privat-dagpenger-subsumsjon-brukt",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)
