from edna.core.execution.context import SimpleStreamingContext
from edna.ingest.streaming import KafkaIngest
from edna.process import BaseProcess
from edna.emit import KafkaEmit
from edna.serializers import StringSerializer
from edna.serializers import KafkaStringSerializer

def main():
    context = SimpleStreamingContext()     # Choose an appropriate context, such as SimpleStreamingContext
    
    ingest_serializer = StringSerializer()  # e.g. KafkaStringSerializer
    emit_serializer = KafkaStringSerializer()    # e.g. StringSerializer
    
    ingest = KafkaIngest(ingest_serializer, 
        kafka_topic=context.getVariable("import_key"),
        bootstrap_server=context.getVariable("bootstrap_server"))    # e.g. KafkaIngest
    process = BaseProcess()                     # e.g. BaseProcess
    emit = KafkaEmit(emit_serializer, 
        kafka_topic=context.getVariable("export_key"),
        bootstrap_server=context.getVariable("bootstrap_server"))           # e.g. KafkaEmit

    context.addIngest(ingest=ingest)        # Registers the ingest primitive
    context.addProcess(process=process)     # Registers the process primitive
    context.addEmit(emit=emit)              # Registers the emit primitive

    context.execute()   # Executes  the context


if __name__ == "__main__":
    main()
