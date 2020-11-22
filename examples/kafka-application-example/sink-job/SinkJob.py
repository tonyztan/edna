from edna.core.execution.context import SimpleStreamingContext
from edna.ingest.streaming import KafkaIngest
from edna.process import BaseProcess
from edna.emit import KafkaEmit
from edna.serializers import StringSerializer
from edna.serializers import KafkaStringSerializer

from edna.ingest.streaming import SimulatedIngest
from edna.process.map import JsonToObject
from edna.serializers.EmptySerializer import EmptyStringSerializer
from edna.serializers.EmptySerializer import EmptyObjectSerializer


class SinkJobProcess(BaseProcess):
    process_name = "SinkJobProcess"

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def process(self, message):
        message = "Processed by Sink Job: " + message
        return [message]

def main():
    # list_of_inserts = ['Processed by Sink Job: ']
    #
    # context2 = SimpleStreamingContext()
    # 
    # ingest2 = SimulatedIngest(serializer=EmptyStringSerializer, stream_list=list_of_inserts)
    #
    # emit_serializer2 = KafkaStringSerializer()    # e.g. StringSerializer
    #
    # process2 = BaseProcess()                     # e.g. BaseProcess
    # emit2 = KafkaEmit(emit_serializer2,
    #     kafka_topic=context2.getVariable("export_key"),
    #     bootstrap_server=context2.getVariable("bootstrap_server"))           # e.g. KafkaEmit
    #
    # context2.addIngest(ingest=ingest2)        # Registers the ingest primitive
    # context2.addProcess(process=process2)     # Registers the process primitive
    # context2.addEmit(emit=emit2)              # Registers the emit primitive
    #
    # context2.execute()   # Executes  the context

    context = SimpleStreamingContext()     # Choose an appropriate context, such as SimpleStreamingContext

    ingest_serializer = StringSerializer()  # e.g. KafkaStringSerializer
    emit_serializer = KafkaStringSerializer()    # e.g. StringSerializer

    ingest = KafkaIngest(ingest_serializer,
        kafka_topic=context.getVariable("import_key"),
        bootstrap_server=context.getVariable("bootstrap_server"))    # e.g. KafkaIngest
    process = SinkJobProcess()                     # e.g. BaseProcess
    emit = KafkaEmit(emit_serializer,
        kafka_topic=context.getVariable("export_key"),
        bootstrap_server=context.getVariable("bootstrap_server"))           # e.g. KafkaEmit

    context.addIngest(ingest=ingest)        # Registers the ingest primitive
    context.addProcess(process=process)     # Registers the process primitive
    context.addEmit(emit=emit)              # Registers the emit primitive

    context.execute()   # Executes  the context


if __name__ == "__main__":
    main()
