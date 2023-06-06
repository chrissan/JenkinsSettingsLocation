def call(Map m){
    switch(m.environment) {
        case "dev":
            springboot.call()
        break
    }
}